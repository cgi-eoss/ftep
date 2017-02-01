package com.cgi.eoss.ftep.model.converters;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 */
public class StringMultimapYamlConverterTest {

    private static final String CONVERTED_YAML = "---\nkey1:\n- \"foo\"\nkey2:\n- \"123\"\n- \"bar2\"\nkey3:\n- \"http://baz/?q=x,y&z={}\"\n";

    private static final Multimap<String, String> MULTIMAP = ImmutableMultimap.of(
            "key1", "foo",
            "key2", "123",
            "key2", "bar2",
            "key3", "http://baz/?q=x,y&z={}"
    );

    private StringMultimapYamlConverter converter;

    @Before
    public void setUp() {
        converter = new StringMultimapYamlConverter();
    }

    @Test
    public void convertToDatabaseColumn() throws Exception {
        String yaml = converter.convertToDatabaseColumn(MULTIMAP);
        assertThat(yaml, is(CONVERTED_YAML));
    }

    @Test
    public void convertToEntityAttribute() throws Exception {
        Multimap<String,String> multimap = converter.convertToEntityAttribute(CONVERTED_YAML);
        assertThat(multimap, is(MULTIMAP));
    }

}