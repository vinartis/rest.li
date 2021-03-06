/*
   Copyright (c) 2013 LinkedIn Corp.

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

package com.linkedin.restli.client;


import com.linkedin.common.callback.Callback;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.DynamicRecordMetadata;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.restli.common.CollectionRequest;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.ResourceSpecImpl;
import com.linkedin.restli.internal.client.RestResponseDecoder;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Moira Tagle
 * @version $Revision: $
 */

public class TestRestClientRequestBuilder
{

  private static final DataMap ENTITY_BODY = new DataMap();
  private static final String  JSON_ENTITY_BODY = "{\"testFieldName\":\"testValue\",\"testInteger\":1}";
  private static final String  PSON_ENTITY_BODY = "#!PSON1\n!\u0081testFieldName\u0000\n\n\u0000\u0000\u0000testValue\u0000\u0083testInteger\u0000\u0002\u0001\u0000\u0000\u0000\u0080";
  private static final String JSON_ENTITIES_BODY = "{\"entities\":{}}";
  private static final String PSON_ENTITIES_BODY = "#!PSON1\n" + "!\u0081entities\u0000 \u0080";
  private static final String  CONTENT_TYPE_HEADER = "Content-Type";
  private static final String  ACCEPT_TYPE_HEADER = "Accept";

  static
  {
    ENTITY_BODY.put("testFieldName", "testValue");
    ENTITY_BODY.put("testInteger", 1);
  }

  @Test(dataProvider = "data")
  public void testGet(RestClient.ContentType contentType,
                      String expectedContentTypeHeader,
                      String expectedRequestBody,
                      String expectedEntitiesBody,
                      List<RestClient.AcceptType> acceptTypes,
                      String expectedAcceptHeader)
    throws URISyntaxException
  {
    RestRequest restRequest = clientGeneratedRequest(GetRequest.class, ResourceMethod.GET, null, contentType, acceptTypes);
    Assert.assertNull(restRequest.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(0, restRequest.getEntity().length());
    Assert.assertEquals(expectedAcceptHeader, restRequest.getHeader(ACCEPT_TYPE_HEADER));

    RestRequest restRequestBatch = clientGeneratedRequest(BatchGetRequest.class, ResourceMethod.BATCH_GET, null, contentType, acceptTypes);
    Assert.assertNull(restRequestBatch.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(0, restRequestBatch.getEntity().length());
    Assert.assertEquals(expectedAcceptHeader, restRequest.getHeader(ACCEPT_TYPE_HEADER));

  }

  @Test(dataProvider = "data")
  public void testFinder(RestClient.ContentType contentType,
                         String expectedContentTypeHeader,
                         String expectedRequestBody,
                         String expectedEntitiesBody,
                         List<RestClient.AcceptType> acceptTypes,
                         String expectedAcceptHeader)
    throws URISyntaxException
  {
    RestRequest restRequest = clientGeneratedRequest(FindRequest.class, ResourceMethod.FINDER, null, contentType, acceptTypes);
    Assert.assertNull(restRequest.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(0, restRequest.getEntity().length());
    Assert.assertEquals(expectedAcceptHeader, restRequest.getHeader(ACCEPT_TYPE_HEADER));

    RestRequest restRequestAll = clientGeneratedRequest(GetAllRequest.class, ResourceMethod.GET_ALL, null, contentType, acceptTypes);
    Assert.assertNull(restRequestAll.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(0, restRequestAll.getEntity().length());
    Assert.assertEquals(expectedAcceptHeader, restRequest.getHeader(ACCEPT_TYPE_HEADER));
  }

  @Test(dataProvider = "data")
  public void testAction(RestClient.ContentType contentType,
                         String expectedContentTypeHeader,
                         String expectedRequestBody,
                         String expectedEntitiesBody,
                         List<RestClient.AcceptType> acceptTypes,
                         String expectedAcceptHeader)
    throws URISyntaxException
  {
    RestRequest restRequest = clientGeneratedRequest(ActionRequest.class, ResourceMethod.ACTION, ENTITY_BODY, contentType, acceptTypes);
    Assert.assertEquals(expectedContentTypeHeader, restRequest.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(expectedRequestBody, restRequest.getEntity().asAvroString());
    Assert.assertEquals(expectedAcceptHeader, restRequest.getHeader(ACCEPT_TYPE_HEADER));

    RestRequest restRequestNoEntity = clientGeneratedRequest(ActionRequest.class, ResourceMethod.ACTION, null, contentType, acceptTypes);
    Assert.assertNull(restRequestNoEntity.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(0, restRequestNoEntity.getEntity().length());
    Assert.assertEquals(expectedAcceptHeader, restRequest.getHeader(ACCEPT_TYPE_HEADER));
  }

  @Test(dataProvider = "data")
  public void testUpdate(RestClient.ContentType contentType,
                         String expectedContentTypeHeader,
                         String expectedRequestBody,
                         String expectedEntitiesBody,
                         List<RestClient.AcceptType> acceptTypes,
                         String expectedAcceptHeader)
    throws URISyntaxException
  {
    RestRequest restRequest = clientGeneratedRequest(UpdateRequest.class, ResourceMethod.UPDATE, ENTITY_BODY, contentType, acceptTypes);
    Assert.assertEquals(expectedContentTypeHeader, restRequest.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(expectedRequestBody, restRequest.getEntity().asAvroString());
    Assert.assertEquals(expectedAcceptHeader, restRequest.getHeader(ACCEPT_TYPE_HEADER));

    RestRequest restRequestBatch = clientGeneratedRequest(BatchUpdateRequest.class, ResourceMethod.BATCH_UPDATE, ENTITY_BODY, contentType, acceptTypes);
    Assert.assertEquals(expectedContentTypeHeader, restRequestBatch.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(restRequestBatch.getEntity().asAvroString(), expectedEntitiesBody);
    Assert.assertEquals(expectedAcceptHeader, restRequestBatch.getHeader(ACCEPT_TYPE_HEADER));

    RestRequest restRequestPartial = clientGeneratedRequest(PartialUpdateRequest.class, ResourceMethod.PARTIAL_UPDATE, ENTITY_BODY, contentType, acceptTypes);
    Assert.assertEquals(expectedContentTypeHeader, restRequestPartial.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(expectedRequestBody, restRequestPartial.getEntity().asAvroString());
    Assert.assertEquals(expectedAcceptHeader, restRequestPartial.getHeader(ACCEPT_TYPE_HEADER));

    RestRequest restRequestBatchPartial = clientGeneratedRequest(BatchPartialUpdateRequest.class, ResourceMethod.BATCH_PARTIAL_UPDATE, ENTITY_BODY, contentType, acceptTypes);
    Assert.assertEquals(expectedContentTypeHeader, restRequestBatchPartial.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(expectedEntitiesBody, restRequestBatchPartial.getEntity().asAvroString());
    Assert.assertEquals(expectedAcceptHeader, restRequestBatchPartial.getHeader(ACCEPT_TYPE_HEADER));
  }

  @Test(dataProvider = "data")
  public void testCreate(RestClient.ContentType contentType,
                         String expectedContentTypeHeader,
                         String expectedRequestBody,
                         String expectedEntitiesBody,
                         List<RestClient.AcceptType> acceptTypes,
                         String expectedAcceptHeader)
    throws URISyntaxException
  {
    RestRequest restRequest = clientGeneratedRequest(CreateRequest.class, ResourceMethod.CREATE, ENTITY_BODY, contentType, acceptTypes);
    Assert.assertEquals(expectedContentTypeHeader, restRequest.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(expectedRequestBody, restRequest.getEntity().asAvroString());
    Assert.assertEquals(expectedAcceptHeader, restRequest.getHeader(ACCEPT_TYPE_HEADER));

    RestRequest restRequestBatch = clientGeneratedRequest(BatchCreateRequest.class, ResourceMethod.BATCH_CREATE, ENTITY_BODY, contentType, acceptTypes);
    Assert.assertEquals(expectedContentTypeHeader, restRequestBatch.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(expectedRequestBody, restRequestBatch.getEntity().asAvroString());
    Assert.assertEquals(expectedAcceptHeader, restRequest.getHeader(ACCEPT_TYPE_HEADER));
  }

  @Test(dataProvider = "data")
  public void testDelete(RestClient.ContentType contentType,
                         String expectedContentTypeHeader,
                         String expectedRequestBody,
                         String expectedEntitiesBody,
                         List<RestClient.AcceptType> acceptTypes,
                         String expectedAcceptHeader)
    throws URISyntaxException
  {
    RestRequest restRequest = clientGeneratedRequest(DeleteRequest.class, ResourceMethod.DELETE, null, contentType, acceptTypes);
    Assert.assertNull(restRequest.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(0, restRequest.getEntity().length());
    Assert.assertEquals(expectedAcceptHeader, restRequest.getHeader(ACCEPT_TYPE_HEADER));

    RestRequest restRequestBatch = clientGeneratedRequest(BatchDeleteRequest.class, ResourceMethod.BATCH_DELETE, null, contentType, acceptTypes);
    Assert.assertNull(restRequestBatch.getHeader(CONTENT_TYPE_HEADER));
    Assert.assertEquals(0, restRequestBatch.getEntity().length());
    Assert.assertEquals(expectedAcceptHeader, restRequest.getHeader(ACCEPT_TYPE_HEADER));
  }

  @DataProvider(name = "data")
  public Object[][] contentTypeData()
  {
    return new Object[][]
      {
        { null,  "application/json", JSON_ENTITY_BODY, JSON_ENTITIES_BODY, null, null }, // default client
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Collections.<RestClient.AcceptType>emptyList(),
          null
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Collections.<RestClient.AcceptType>emptyList(),
          null
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Collections.singletonList(RestClient.AcceptType.ANY),
          "*/*"
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Collections.singletonList(RestClient.AcceptType.ANY),
          "*/*"
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Collections.singletonList(RestClient.AcceptType.JSON),
          "application/json"
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Collections.singletonList(RestClient.AcceptType.JSON),
          "application/json"
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Collections.singletonList(RestClient.AcceptType.PSON),
          "application/x-pson"
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Collections.singletonList(RestClient.AcceptType.PSON),
          "application/x-pson"
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.PSON),
          "application/json;q=1.0,application/x-pson;q=0.9"
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.PSON),
          "application/json;q=1.0,application/x-pson;q=0.9"
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.ANY),
          "application/json;q=1.0,*/*;q=0.9"
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.ANY),
          "application/json;q=1.0,*/*;q=0.9"
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.JSON),
          "application/x-pson;q=1.0,application/json;q=0.9"
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.JSON),
          "application/x-pson;q=1.0,application/json;q=0.9"
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.ANY),
          "application/x-pson;q=1.0,*/*;q=0.9"
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.PSON, RestClient.AcceptType.ANY),
          "application/x-pson;q=1.0,*/*;q=0.9"
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.JSON),
          "*/*;q=1.0,application/json;q=0.9"
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.JSON),
          "*/*;q=1.0,application/json;q=0.9"
        },
        {
          RestClient.ContentType.JSON,
          "application/json",
          JSON_ENTITY_BODY,
          JSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.PSON),
          "*/*;q=1.0,application/x-pson;q=0.9"
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.ANY, RestClient.AcceptType.PSON),
          "*/*;q=1.0,application/x-pson;q=0.9"
        },
        {
          RestClient.ContentType.PSON,
          "application/x-pson",
          PSON_ENTITY_BODY,
          PSON_ENTITIES_BODY,
          Arrays.asList(RestClient.AcceptType.JSON, RestClient.AcceptType.PSON, RestClient.AcceptType.ANY),
          "application/json;q=1.0,application/x-pson;q=0.9,*/*;q=0.8"
        },
      };
  }

  private void setCommonExpectations(Request mockRequest,
                                     ResourceMethod method,
                                     RecordTemplate mockRecordTemplate,
                                     RestResponseDecoder mockResponseDecoder)
  {
    EasyMock.expect(mockRequest.getMethod()).andReturn(method).anyTimes();
    EasyMock.expect(mockRequest.hasUri()).andReturn(false).once();
    EasyMock.expect(mockRequest.getPathKeys()).andReturn(Collections.<String, String>emptyMap()).once();
    EasyMock.expect(mockRequest.getQueryParamsObjects()).andReturn(Collections.emptyMap()).once();
    EasyMock.expect(mockRequest.getBaseUriTemplate()).andReturn("/foo").times(2);
    EasyMock.expect(mockRequest.getServiceName()).andReturn("foo").once();
    EasyMock.expect(mockRequest.getResponseDecoder()).andReturn(mockResponseDecoder).once();
    EasyMock.expect(mockRequest.getHeaders()).andReturn(Collections.<String, String>emptyMap()).once();
  }

  private void buildInputForBatchPathAndUpdate(Request mockRequest)
  {
    CollectionRequest mockCollectionRequest = EasyMock.createMock(CollectionRequest.class);
    EasyMock.expect(mockCollectionRequest.getElements()).andReturn(Collections.emptyList()).once();
    EasyMock.expect(mockRequest.getInputRecord()).andReturn(mockCollectionRequest).times(2);
    EasyMock.replay(mockCollectionRequest);
    ResourceSpec resourceSpec = new ResourceSpecImpl(Collections.<ResourceMethod> emptySet(),
                                                     Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                     Collections.<String, DynamicRecordMetadata> emptyMap(),
                                                     null,
                                                     null,
                                                     null,
                                                     RecordTemplate.class,
                                                     Collections.<String, CompoundKey.TypeInfo> emptyMap());
    EasyMock.expect(mockRequest.getResourceSpec()).andReturn(resourceSpec).once();
  }

  @SuppressWarnings("unchecked")
  private <T extends Request> RestRequest clientGeneratedRequest(Class<T> requestClass,
                                                                 ResourceMethod method,
                                                                 DataMap entityBody,
                                                                 RestClient.ContentType contentType,
                                                                 List<RestClient.AcceptType> acceptTypes)
    throws URISyntaxException
  {
    // massive setup...
    Client mockClient = EasyMock.createMock(Client.class);
    @SuppressWarnings({"rawtypes"})
    Request<?> mockRequest = EasyMock.createMock(requestClass);
    RecordTemplate mockRecordTemplate = EasyMock.createMock(RecordTemplate.class);
    @SuppressWarnings({"rawtypes"})
    RestResponseDecoder mockResponseDecoder = EasyMock.createMock(RestResponseDecoder.class);

    setCommonExpectations(mockRequest, method, mockRecordTemplate, mockResponseDecoder);

    if (method == ResourceMethod.BATCH_PARTIAL_UPDATE || method == ResourceMethod.BATCH_UPDATE)
    {
      buildInputForBatchPathAndUpdate(mockRequest);
    }
    else
    {
      EasyMock.expect(mockRequest.getInputRecord()).andReturn(mockRecordTemplate).times(2);
      EasyMock.expect(mockRequest.getResourceSpec()).andReturn(new ResourceSpecImpl()).once();
    }

    if (method == ResourceMethod.GET)
    {
      EasyMock.expect(((GetRequest)mockRequest).getObjectId()).andReturn(null).once();
      EasyMock.expect(((GetRequest)mockRequest).getResourceSpec()).andReturn(new ResourceSpecImpl()).once();
      EasyMock.expect(mockRequest.getMethodName()).andReturn(null);
    }
    else if (method == ResourceMethod.BATCH_GET)
    {
      EasyMock.expect(mockRequest.getMethodName()).andReturn(null);
    }
    else if (method == ResourceMethod.ACTION)
    {
      EasyMock.expect(((ActionRequest)mockRequest).getId()).andReturn(null);
      EasyMock.expect(mockRequest.getMethodName()).andReturn("testAction");
    }
    else if (method == ResourceMethod.FINDER)
    {
      EasyMock.expect(((FindRequest)mockRequest).getAssocKey()).andReturn(new CompoundKey());
      EasyMock.expect(mockRequest.getMethodName()).andReturn("testFinder");
    }
    else if (method == ResourceMethod.GET_ALL)
    {
      EasyMock.expect(((GetAllRequest)mockRequest).getAssocKey()).andReturn(new CompoundKey());
      EasyMock.expect(mockRequest.getMethodName()).andReturn(null);
    }
    else if (method == ResourceMethod.UPDATE)
    {
      EasyMock.expect(((UpdateRequest) mockRequest).getResourceSpec()).andReturn(new ResourceSpecImpl()).once();
      EasyMock.expect(((UpdateRequest)mockRequest).getId()).andReturn(null);
      EasyMock.expect(mockRequest.getMethodName()).andReturn(null);
    }
    else if (method == ResourceMethod.PARTIAL_UPDATE)
    {
      EasyMock.expect(mockRequest.getResourceSpec()).andReturn(new ResourceSpecImpl()).times(2);
      EasyMock.expect(((PartialUpdateRequest)mockRequest).getId()).andReturn(null);
      EasyMock.expect(mockRequest.getMethodName()).andReturn(null);
    }
    else if (method == ResourceMethod.DELETE)
    {
      EasyMock.expect(((DeleteRequest)mockRequest).getResourceSpec()).andReturn(new ResourceSpecImpl()).once();
      EasyMock.expect(((DeleteRequest)mockRequest).getId()).andReturn(null);
      EasyMock.expect(mockRequest.getMethodName()).andReturn(null);
    }
    else
    {
      EasyMock.expect(mockRequest.getMethodName()).andReturn(null);
    }

    EasyMock.expect(mockRecordTemplate.data()).andReturn(entityBody).once();

    Capture<RestRequest> restRequestCapture = new Capture<RestRequest>();

    mockClient.restRequest(EasyMock.capture(restRequestCapture),
                           (RequestContext) EasyMock.anyObject(),
                           (Callback<RestResponse>) EasyMock.anyObject());
    EasyMock.expectLastCall().once();

    EasyMock.replay(mockClient, mockRequest, mockRecordTemplate);

    // do work!
    String host = "host";
    RestClient restClient;
    if (acceptTypes == null)
    {
      restClient = new RestClient(mockClient, host);
    }
    else if (contentType == null)
    {
      restClient = new RestClient(mockClient, host, acceptTypes);
    }
    else
    {
      restClient = new RestClient(mockClient, host, contentType, acceptTypes);
    }

    restClient.sendRequest(mockRequest);

    return restRequestCapture.getValue();
  }

}
