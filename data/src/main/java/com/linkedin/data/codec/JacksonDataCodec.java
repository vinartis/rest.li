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

package com.linkedin.data.codec;


import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonLocation;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.PrettyPrinter;

/**
 * A JSON codec that uses Jackson for serialization and de-serialization.
 *
 * @author slim
 */
public class JacksonDataCodec implements DataCodec
{
  public JacksonDataCodec()
  {
    this(new JsonFactory());
  }

  public JacksonDataCodec(JsonFactory jsonFactory)
  {
    _jsonFactory = jsonFactory;
    setAllowComments(true);
  }

  public void setAllowComments(boolean allowComments)
  {
    _jsonFactory.configure(JsonParser.Feature.ALLOW_COMMENTS, allowComments);
    _allowComments = allowComments;
  }

  public PrettyPrinter getPrettyPrinter()
  {
    return _prettyPrinter;
  }

  public void setPrettyPrinter(PrettyPrinter prettyPrinter)
  {
    _prettyPrinter = prettyPrinter;
  }

  @Override
  public String getStringEncoding()
  {
    return _jsonEncoding.getJavaName();
  }

  @Override
  public byte[] mapToBytes(DataMap map) throws IOException
  {
    return objectToBytes(map);
  }

  protected byte[] objectToBytes(Object object) throws IOException
  {
    ByteArrayOutputStream out = new ByteArrayOutputStream(_defaultBufferSize);
    writeObject(object, out);
    return out.toByteArray();
  }

  @Override
  public DataMap bytesToMap(byte[] input) throws IOException
  {
    Parser parser = new Parser();
    return parser.parse(input);
  }

  @Override
  public void writeMap(DataMap map, OutputStream out) throws IOException
  {
    writeObject(map, out);
  }

  protected JsonGenerator createJsonGenerator(OutputStream out) throws IOException
  {
    final JsonGenerator generator = _jsonFactory.createJsonGenerator(out, _jsonEncoding);
    if (_prettyPrinter != null)
    {
      generator.setPrettyPrinter(_prettyPrinter);
    }
    return generator;
  }

  protected void writeObject(Object object, OutputStream out) throws IOException
  {
    JsonGenerator generator = createJsonGenerator(out);
    JsonTraverseCallback callback = new JsonTraverseCallback(generator);
    Data.traverse(object, callback);
    generator.flush();
    generator.close();
  }

  @Override
  public DataMap readMap(InputStream in) throws IOException
  {
    Parser parser = new Parser();
    return parser.parse(in);
  }

  @Deprecated
  public List<Object> parse(InputStream in, StringBuilder mesg) throws IOException
  {
    return parse(in, mesg, null);
  }

  /**
   * Reads an {@link InputStream} and parses its contents into a list of Data objects.
   *
   * @param in provides the {@link InputStream}
   * @param mesg provides the {@link StringBuilder} to store validation error messages,
   *             such as duplicate keys in the same {@link DataMap}.
   * @param locationMap provides where to store the mapping of a Data object
   *                    to its location in the in the {@link InputStream}. may be
   *                    {@code null} if this mapping is not needed by the caller.
   *                    This map should usually be an {@link IdentityHashMap}.
   * @return the list of Data objects parsed from the {@link InputStream}.
   * @throws IOException if there is a syntax error in the input.
   */
  public List<Object> parse(InputStream in, StringBuilder mesg, Map<Object, DataLocation> locationMap)
    throws IOException
  {
    Parser parser = new Parser(true);
    return parser.parse(in, mesg, locationMap);
  }

  public void objectToJsonGenerator(Object object, JsonGenerator generator) throws IOException
  {
    JsonTraverseCallback callback = new JsonTraverseCallback(generator);
    Data.traverse(object, callback);
  }

  protected static class JsonTraverseCallback implements Data.TraverseCallback
  {
    protected JsonTraverseCallback(JsonGenerator jsonGenerator)
    {
      _jsonGenerator = jsonGenerator;
    }

    @Override
    public Iterable<Map.Entry<String,Object>> orderMap(DataMap map)
    {
      return map.entrySet();
    }

    @Override
    public void nullValue() throws JsonGenerationException, IOException
    {
      _jsonGenerator.writeNull();
    }

    @Override
    public void booleanValue(boolean value) throws JsonGenerationException, IOException
    {
      _jsonGenerator.writeBoolean(value);
    }

    @Override
    public void integerValue(int value) throws JsonGenerationException, IOException
    {
      _jsonGenerator.writeNumber(value);
    }

    @Override
    public void longValue(long value) throws JsonGenerationException, IOException
    {
      _jsonGenerator.writeNumber(value);
    }

    @Override
    public void floatValue(float value) throws JsonGenerationException, IOException
    {
      _jsonGenerator.writeNumber(value);
    }

    @Override
    public void doubleValue(double value) throws JsonGenerationException, IOException
    {
      _jsonGenerator.writeNumber(value);
    }

    @Override
    public void stringValue(String value) throws JsonGenerationException, IOException
    {
      _jsonGenerator.writeString(value);
    }

    @Override
    public void byteStringValue(ByteString value) throws JsonGenerationException, IOException
    {
      _jsonGenerator.writeString(value.asAvroString());
    }

    @Override
    public void illegalValue(Object value) throws DataEncodingException
    {
      throw new DataEncodingException("Illegal value encountered: " + value);
    }

    @Override
    public void emptyMap() throws JsonGenerationException, IOException
    {
      _jsonGenerator.writeStartObject();
      _jsonGenerator.writeEndObject();
    }

    @Override
    public void startMap(DataMap map) throws JsonGenerationException, IOException
    {
      _jsonGenerator.writeStartObject();
    }

    @Override
    public void key(String key) throws JsonGenerationException, IOException
    {
      _jsonGenerator.writeFieldName(key);
    }

    @Override
    public void endMap() throws JsonGenerationException, IOException
    {
      _jsonGenerator.writeEndObject();
    }

    @Override
    public void emptyList() throws JsonGenerationException, IOException
    {
      _jsonGenerator.writeStartArray();
      _jsonGenerator.writeEndArray();
    }

    @Override
    public void startList(DataList list) throws JsonGenerationException, IOException
    {
      _jsonGenerator.writeStartArray();
    }

    @Override
    public void index(int index)
    {
    }

    @Override
    public void endList() throws JsonGenerationException, IOException
    {
      _jsonGenerator.writeEndArray();
    }

    private final JsonGenerator _jsonGenerator;
  }

  // http://jira.codehaus.org/browse/JACKSON-230
  // http://jira.codehaus.org/browse/JACKSON-491
  // begin of workaround code
  private static final boolean JACKSON_230_WORKAROUND;
  private static final boolean JACKSON_491_WORKAROUND;
  private static final long MAX_INT = Integer.MAX_VALUE;
  private static final long MIN_INT = Integer.MIN_VALUE;
  private static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);
  private static final BigInteger MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);
  static
  {
    boolean isJackson230Bug;
    boolean isJackson491Bug;
    String json = "{ \"int\" : " + Integer.MAX_VALUE + ", \"long\" : 1323372036854775807 }";
    try
    {
      JsonFactory factory = new JsonFactory();
      JsonParser parser = factory.createJsonParser(new ByteArrayInputStream(json.getBytes()));
      JsonToken token = parser.nextToken();
      assert(token == JsonToken.START_OBJECT);
      token = parser.nextToken();
      assert(token == JsonToken.FIELD_NAME);
      assert(parser.getCurrentName().equals("int"));
      token = parser.nextToken();
      assert(token == JsonToken.VALUE_NUMBER_INT);
      JsonParser.NumberType numberType = parser.getNumberType();
      isJackson230Bug = (numberType == JsonParser.NumberType.LONG);
      assert(isJackson230Bug || numberType == JsonParser.NumberType.INT);
      token = parser.nextToken();
      assert(token == JsonToken.FIELD_NAME);
      assert(parser.getCurrentName().equals("long"));
      token = parser.nextToken();
      assert(token == JsonToken.VALUE_NUMBER_INT);
      numberType = parser.getNumberType();
      isJackson491Bug = (numberType == JsonParser.NumberType.BIG_INTEGER);
      assert(isJackson491Bug || numberType == JsonParser.NumberType.LONG);
    }
    catch (IOException e)
    {
      throw new IllegalStateException("Parsing " + json + " should not fail", e);
    }
    JACKSON_230_WORKAROUND = isJackson230Bug;
    JACKSON_491_WORKAROUND = isJackson491Bug;
  }
  // end of workaround code

  private static class Location implements DataLocation
  {
    private Location(JsonLocation location)
    {
      _location = location;
    }
    public int getColumn()
    {
      return _location.getColumnNr();
    }
    public int getLine()
    {
      return _location.getLineNr();
    }

    @Override
    public int compareTo(DataLocation other)
    {
      return (int) (_location.getCharOffset() - ((Location) other)._location.getCharOffset());
    }

    @Override
    public String toString()
    {
      return getLine() + "," + getColumn();
    }
    private final JsonLocation _location;
  }

  private class Parser
  {
    private StringBuilder _errorBuilder = null;
    private JsonParser _parser = null;
    private boolean _debug = false;
    private Deque<Object> _nameStack = null;
    private Map<Object, DataLocation> _locationMap = null;

    Parser()
    {
      this(false);
    }

    Parser(boolean debug)
    {
      _debug = debug;
    }

    /**
     * Returns map of location to object, sorted by location.
     *
     * May be used to debug location map.
     */
    private Map<DataLocation, Object> sortedLocationsMap()
    {
      if (_locationMap == null)
        return null;

      TreeMap<DataLocation, Object> sortedMap = new TreeMap<DataLocation, Object>();
      for (Map.Entry<Object, DataLocation> e : _locationMap.entrySet())
      {
        sortedMap.put(e.getValue(), e.getKey());
      }
      return sortedMap;
    }

    List<Object> parse(InputStream in, StringBuilder mesg, Map<Object, DataLocation> locationMap)
      throws JsonParseException, IOException
    {
      _locationMap = locationMap;

      DataList list = new DataList();
      _errorBuilder = mesg;
      if (_debug)
      {
         _nameStack = new ArrayDeque<Object>();
      }

      _parser = _jsonFactory.createJsonParser(in);
      JsonToken token;
      while ((token = _parser.nextToken()) != null)
      {
        parse(list, null, null, token);
      }
      _errorBuilder = null;

      return list;
    }

    DataMap parse(byte[] input) throws JsonParseException, IOException
    {
      return parse(new ByteArrayInputStream(input));
    }

    DataMap parse(InputStream in) throws IOException
    {
      _errorBuilder = null;
      if (_debug)
      {
         _nameStack = new ArrayDeque<Object>();
      }

      _parser = _jsonFactory.createJsonParser(in);
      JsonToken token = _parser.nextToken();
      if (! JsonToken.START_OBJECT.equals(token))
      {
        throw new DataDecodingException("Missing JSON start token");
      }
      DataMap map = new DataMap();
      parseDataMap(map);
      if (_errorBuilder != null)
      {
        map.addError(_errorBuilder.toString());
      }
      return map;
    }

    private DataLocation currentDataLocation()
    {
      return _locationMap == null ? null : new Location(_parser.getTokenLocation());
    }

    private void saveDataLocation(Object o, DataLocation location)
    {
      if (_locationMap != null && o != null)
      {
        assert(location != null);
        _locationMap.put(o, location);
      }
    }

    private Object parse(DataList parentList, DataMap parentMap, String name, JsonToken token)
        throws JsonParseException, IOException
    {
      if (token == null)
      {
        throw new DataDecodingException("Missing JSON token");
      }
      Object value;
      DataLocation location = currentDataLocation();
      switch (token)
      {
        case START_OBJECT:
          DataMap childMap = new DataMap();
          value = childMap;
          updateParent(parentList, parentMap, name, childMap);
          parseDataMap(childMap);
          break;
        case START_ARRAY:
          DataList childList = new DataList();
          value = childList;
          updateParent(parentList, parentMap, name, childList);
          parseDataList(childList);
          break;
        default:
          value = parsePrimitive(token);
          if (value != null)
          {
            updateParent(parentList, parentMap, name, value);
          }
          break;
      }
      saveDataLocation(value, location);
      return value;
    }

    private void updateParent(DataList parentList, DataMap parentMap, String name, Object value)
    {
      if (parentMap != null)
      {
        Object replaced = parentMap.put(name, value);
        if (replaced != null)
        {
          if (_errorBuilder == null)
          {
            _errorBuilder = new StringBuilder();
          }
          _errorBuilder.append(new Location(_parser.getTokenLocation())).append(": \"").append(name).append("\" defined more than once.\n");
        }
      }
      else
      {
        parentList.add(value);
      }
    }

    private Object parsePrimitive(JsonToken token) throws JsonParseException, IOException
    {
      Object object;
      JsonParser.NumberType numberType = null;
      switch (token) {
        case VALUE_STRING:
          object = _parser.getText();
          break;
        case VALUE_NUMBER_INT:
        case VALUE_NUMBER_FLOAT:
          numberType = _parser.getNumberType();
          switch (numberType) {
            case INT:
              object = _parser.getIntValue();
              break;
            case LONG:
              if (JACKSON_230_WORKAROUND)
              {
                // Jackson too eagerly use longs for ints
                // http://jira.codehaus.org/browse/JACKSON-230
                // cast to Object required to avoid numeric conversion
                long longValue = _parser.getLongValue();
                object = (MIN_INT <= longValue && longValue <= MAX_INT) ? (Object) Integer.valueOf((int) longValue) : (Object) Long.valueOf(longValue);
              }
              else
              {
                object = _parser.getLongValue();
              }
              break;
            case FLOAT:
              object = _parser.getFloatValue();
              break;
            case DOUBLE:
              object = _parser.getDoubleValue();
              break;
            case BIG_INTEGER:
              if (JACKSON_230_WORKAROUND || JACKSON_491_WORKAROUND)
              {
                // Jackson too eagerly use big integers for long
                // http://jira.codehaus.org/browse/JACKSON-230
                // http://jira.codehaus.org/browse/JACKSON-491
                BigInteger bigInteger = _parser.getBigIntegerValue();
                if (bigInteger.compareTo(MIN_LONG) >= 0 && bigInteger.compareTo(MAX_LONG) <= 0)
                {
                  object = bigInteger.longValue();
                  break;
                }
              }
              // repeat to avoid fall through warning
              error(token, numberType);
              object = null;
              break;
            case BIG_DECIMAL:
            default:
              error(token, numberType);
              object = null;
              break;
          }
          break;
        case VALUE_TRUE:
          object = Boolean.TRUE;
          break;
        case VALUE_FALSE:
          object = Boolean.FALSE;
          break;
        case VALUE_NULL:
          object = Data.NULL;
          break;
        default:
          error(token, null);
          object = null;
          break;
      }
      return object;
    }

    private void parseDataMap(DataMap map) throws JsonParseException, IOException
    {
      while (_parser.nextToken() != JsonToken.END_OBJECT)
      {
        String key = _parser.getCurrentName();
        if (_debug)
        {
          _nameStack.addLast(key);
        }
        JsonToken token = _parser.nextToken();
        parse(null, map, key, token);
        if (_debug)
        {
          _nameStack.removeLast();
        }
      }
    }

    private void parseDataList(DataList list) throws JsonParseException, IOException
    {
      JsonToken token;
      int index = 0;
      while ((token = _parser.nextToken()) != JsonToken.END_ARRAY)
      {
        if (_debug)
        {
          _nameStack.addLast(index);
          index++;
        }
        parse(list, null, null, token);
        if (_debug)
        {
          _nameStack.removeLast();
        }
      }
    }

    private void error(JsonToken token, JsonParser.NumberType type) throws JsonParseException, IOException
    {
      if (_errorBuilder == null)
      {
        _errorBuilder = new StringBuilder();
      }
      _errorBuilder.append(_parser.getTokenLocation()).append(": ");
      if (_debug)
      {
        _errorBuilder.append("name: ");
        Data.appendNames(_errorBuilder, _nameStack);
        _errorBuilder.append(", ");
      }
      _errorBuilder.append("value: ").append(_parser.getText()).append(", token: ").append(token);
      if (type != null)
      {
        _errorBuilder.append(", number type: ").append(type);
      }
      _errorBuilder.append(" not parsed.\n");
    }
  }

  protected boolean _allowComments;
  protected PrettyPrinter _prettyPrinter;
  protected JsonFactory _jsonFactory;
  protected int _defaultBufferSize = 4096;
  protected JsonEncoding _jsonEncoding = JsonEncoding.UTF8;
}