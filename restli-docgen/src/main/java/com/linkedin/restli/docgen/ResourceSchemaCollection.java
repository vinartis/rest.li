/*
   Copyright (c) 2012 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.linkedin.restli.docgen;

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.RestLiInternalException;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.model.ResourceModelEncoder;
import com.linkedin.restli.internal.server.model.ResourceModelEncoder.NullDocsProvider;
import com.linkedin.restli.restspec.ActionSchema;
import com.linkedin.restli.restspec.ActionSchemaArray;
import com.linkedin.restli.restspec.ActionsSetSchema;
import com.linkedin.restli.restspec.AssociationSchema;
import com.linkedin.restli.restspec.CollectionSchema;
import com.linkedin.restli.restspec.EntitySchema;
import com.linkedin.restli.restspec.FinderSchema;
import com.linkedin.restli.restspec.FinderSchemaArray;
import com.linkedin.restli.restspec.ParameterSchema;
import com.linkedin.restli.restspec.ResourceSchema;
import com.linkedin.restli.restspec.RestMethodSchema;
import com.linkedin.restli.restspec.RestMethodSchemaArray;
import com.linkedin.restli.restspec.RestSpecCodec;
import com.linkedin.restli.server.ResourceLevel;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A collection of ResourceSchema, supporting visitor-style iteration. Each ResourceSchema
 * (and sub-resource) is identified by a dot-delimited path e.g. "groups" or "groups.contacts"
 *
 * @author dellamag
 */
public class ResourceSchemaCollection
{
  /**
   * Create {@link ResourceSchemaCollection} from specified root {@link ResourceModel}.
   * All resources will be recursively traversed to discover subresources.
   * Root resources not specified are excluded.
   *
   * @param rootResources root resources in ResourceModel type
   * @return constructed ResourceSchemaCollection
   */
  public static ResourceSchemaCollection createFromResourceModels(Map<String, ResourceModel> rootResources)
  {
    final ResourceModelEncoder encoder = new ResourceModelEncoder(new NullDocsProvider());
    final Map<String, ResourceSchema> schemaMap = new TreeMap<String, ResourceSchema>();
    for (ResourceModel resource: rootResources.values())
    {
      schemaMap.put(resource.getName(), encoder.buildResourceSchema(resource));
    }

    return new ResourceSchemaCollection(schemaMap);
  }

  /**
   * Create {@link ResourceSchemaCollection} from idl files.
   *
   * @param restspecSearchPaths file system paths to search for idl files
   * @return constructed ResourceSchemaCollection
   */
  public static ResourceSchemaCollection createFromIdls(String[] restspecSearchPaths)
  {
    final RestSpecCodec codec = new RestSpecCodec();
    final Map<String, ResourceSchema> resourceSchemaMap = new HashMap<String, ResourceSchema>();

    for (String path : restspecSearchPaths)
    {
      final File dir = new File(path);
      if (! dir.isDirectory())
      {
        throw new IllegalArgumentException(String.format("path '%s' is not a directory", dir.getAbsolutePath()));
      }
      final File[] idlFiles = dir.listFiles(new FileFilter() {
        @Override
        public boolean accept(File pathname)
        {
          return pathname.getName().endsWith(RestConstants.RESOURCE_MODEL_FILENAME_EXTENSION);
        }
      });

      for (File idlFile : idlFiles)
      {
        try
        {
          final FileInputStream is = new FileInputStream(idlFile);
          final ResourceSchema resourceSchema = codec.readResourceSchema(is);
          resourceSchemaMap.put(resourceSchema.getName(), resourceSchema);
        }
        catch (IOException e)
        {
          throw new RestLiInternalException(String.format("Error loading restspec IDL file '%s'", idlFile.getName()), e);
        }
      }
    }

    return new ResourceSchemaCollection(resourceSchemaMap);
  }

  /**
   * Store the specified root resources plus the discover subresources
   * @param rootResources root resources in {@link ResourceSchema} type
   */
  public ResourceSchemaCollection(Map<String, ResourceSchema> rootResources)
  {
    _allResources = new TreeMap<String, ResourceSchema>(rootResources);
    _subResources = new IdentityHashMap<ResourceSchema, List<ResourceSchema>>();
    final Map<String, ResourceSchema> flattenSubResources = new TreeMap<String, ResourceSchema>();

    final ResourceSchemaVisitior visitor = new BaseResourceSchemaVisitor()
    {
      @Override
      public void visitResourceSchema(VisitContext context,
                                      ResourceSchema resourceSchema)
      {
        if (!_allResources.containsKey(resourceSchema.getName()))
        {
          flattenSubResources.put(context.getResourcePath(), resourceSchema);

          final List<ResourceSchema> hierarchy = context.getResourceSchemaHierarchy();
          final ResourceSchema parent = hierarchy.get(hierarchy.size() - 2);
          List<ResourceSchema> subList = _subResources.get(parent);
          if (subList == null)
          {
            subList = new ArrayList<ResourceSchema>();
            _subResources.put(parent, subList);
          }
          subList.add(resourceSchema);
        }
      }
    };
    this.visit(visitor);
    _allResources.putAll(flattenSubResources);
  }

  /**
   * Retrieve the resource schema for the specified path.
   *
   * @param resourcePath for root resources, the path is the name of the resource;
   *                     for subresource, the path is the fully-qualitied resource name, delimited with "."
   * @return schema of the resource
   */
  public ResourceSchema getResource(String resourcePath)
  {
    return _allResources.get(resourcePath);
  }

  /**
   * @return map from the resource path to both root resources and all discovered subresources
   */
  public Map<String, ResourceSchema> getResources()
  {
    return _allResources;
  }

  /**
   * @param ancestorSchema a root resource schema
   * @return schema of all subresources that are the descendants of the specified resource
   */
  public List<ResourceSchema> getSubResources(ResourceSchema ancestorSchema)
  {
    return _subResources.get(ancestorSchema);
  }

  /**
   * @param visitor {@link ResourceSchemaVisitior} to visit all resource schemas with the specified visitor
   */
  public void visit(ResourceSchemaVisitior visitor)
  {
    for (ResourceSchema schema : _allResources.values())
    {
      processResourceSchema(visitor, new ArrayList<ResourceSchema>(), schema);
    }
  }

  private void processResourceSchema(ResourceSchemaVisitior visitor,
                                     List<ResourceSchema> hierarchy,
                                     ResourceSchema resourceSchema)
  {
    hierarchy.add(resourceSchema);

    final ResourceSchemaVisitior.VisitContext context = buildContext(hierarchy);
    visitor.visitResourceSchema(context, resourceSchema);

    if (resourceSchema.hasCollection())
    {
      final CollectionSchema collectionSchema = resourceSchema.getCollection();
      visitor.visitCollectionResource(context, collectionSchema);

      processRestMethods(visitor, context, collectionSchema, collectionSchema.getMethods());
      processFinders(visitor, context, collectionSchema, collectionSchema.getFinders());
      processActions(visitor, context, collectionSchema, collectionSchema.getActions());

      processEntitySchema(visitor, context, collectionSchema.getEntity());
    }
    else if (resourceSchema.hasAssociation())
    {
      final AssociationSchema associationSchema = resourceSchema.getAssociation();
      visitor.visitAssociationResource(context, associationSchema);

      processRestMethods(visitor, context, associationSchema, associationSchema.getMethods());
      processFinders(visitor, context, associationSchema, associationSchema.getFinders());
      processActions(visitor, context, associationSchema, associationSchema.getActions());

      processEntitySchema(visitor, context, associationSchema.getEntity());
    }
    else if (resourceSchema.hasActionsSet())
    {
      final ActionsSetSchema actionsSet = resourceSchema.getActionsSet();
      visitor.visitActionSetResource(context, actionsSet);

      processActions(visitor, context, actionsSet, actionsSet.getActions());
    }

    hierarchy.remove(hierarchy.size() - 1);
  }


  private void processEntitySchema(ResourceSchemaVisitior visitor,
                                   ResourceSchemaVisitior.VisitContext context,
                                   EntitySchema entitySchema)
  {
    visitor.visitEntityResource(context, entitySchema);

    processActions(visitor, context, entitySchema, entitySchema.getActions());

    if (entitySchema.hasSubresources())
    {
      for (ResourceSchema resourceSchema : entitySchema.getSubresources())
      {
        processResourceSchema(visitor, context.getResourceSchemaHierarchy(), resourceSchema);
      }
    }
  }

  private void processRestMethods(ResourceSchemaVisitior visitor,
                                  ResourceSchemaVisitior.VisitContext context,
                                  RecordTemplate containingResourceType,
                                  RestMethodSchemaArray methods)
  {
    if (methods != null)
    {
      for (RestMethodSchema restMethodSchema : methods)
      {
        visitor.visitRestMethod(context, containingResourceType, restMethodSchema);

        if (restMethodSchema.hasParameters())
        {
          for (ParameterSchema parameterSchema : restMethodSchema.getParameters())
          {
            visitor.visitParameter(context,
                                   containingResourceType,
                                   restMethodSchema,
                                   parameterSchema);
          }
        }
      }
    }
  }

  private void processFinders(ResourceSchemaVisitior visitor,
                              ResourceSchemaVisitior.VisitContext context,
                              RecordTemplate containingResourceType,
                              FinderSchemaArray finders)
  {
    if (finders != null)
    {
      for (FinderSchema finderSchema : finders)
      {
        visitor.visitFinder(context, containingResourceType, finderSchema);

        if (finderSchema.hasParameters())
        {
          for (ParameterSchema parameterSchema : finderSchema.getParameters())
          {
            visitor.visitParameter(context,
                                   containingResourceType,
                                   finderSchema,
                                   parameterSchema);
          }
        }
      }
    }
  }


  private void processActions(ResourceSchemaVisitior visitor,
                              ResourceSchemaVisitior.VisitContext context,
                              RecordTemplate containingResourceType,
                              ActionSchemaArray actions)
  {
    if (actions != null)
    {
      final ResourceLevel resourceLevel = (EntitySchema.class.equals(containingResourceType.getClass()) ?
                                           ResourceLevel.ENTITY :
                                           ResourceLevel.COLLECTION);

      for (ActionSchema actionSchema : actions)
      {
        visitor.visitAction(context, containingResourceType, resourceLevel, actionSchema);

        if (actionSchema.hasParameters())
        {
          for (ParameterSchema parameterSchema : actionSchema.getParameters())
          {
            visitor.visitParameter(context,
                                   containingResourceType,
                                   actionSchema,
                                   parameterSchema);
          }
        }
      }
    }
  }

  private ResourceSchemaVisitior.VisitContext buildContext(List<ResourceSchema> hierarchy)
  {
    final StringBuilder resourcePath = new StringBuilder();
    for (ResourceSchema resourceSchema : hierarchy)
    {
      resourcePath.append(resourceSchema.getName()).append(".");
    }
    resourcePath.deleteCharAt(resourcePath.length() - 1);

    return new ResourceSchemaVisitior.VisitContext(hierarchy, resourcePath.toString());
  }

  private final Map<String, ResourceSchema> _allResources;
  private final Map<ResourceSchema, List<ResourceSchema>> _subResources;
}