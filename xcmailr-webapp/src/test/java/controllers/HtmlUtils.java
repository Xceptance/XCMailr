/*
 * Copyright (c) 2013-2023 Xceptance Software Technologies GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package controllers;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

public class HtmlUtils
{
    /**
     * takes an HTML Document as String and parses it <br/>
     * it will search for all input-fields and return their names and values as hashmap
     * 
     * @param result
     *            - the HTML Document as String
     * @return a Hashmap of all names and values from input-fields
     */
    public static Map<String, String> readInputFormData(String result)
    {
        Reader reader = new StringReader(result);
        HTMLEditorKit.Parser parser = new ParserDelegator();
        try
        {

            HTMLFormParser formParser = new HTMLFormParser();
            parser.parse(reader, formParser, true);
            reader.close();

            return formParser.map;
        }
        catch (IOException e)
        {
            return new HashMap<String, String>();
        }
    }

    /**
     * inner class to parse the input & selection fields
     * 
     * @author pthum
     */
    private static class HTMLFormParser extends HTMLEditorKit.ParserCallback
    {
        public Map<String, String> map = new HashMap<String, String>();

        private String lastSelect = "";

        public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attr, int pos)
        {
            if (tag == HTML.Tag.INPUT)
            { // found an input-field
                String key = (String) attr.getAttribute(HTML.Attribute.NAME);
                String value = (String) attr.getAttribute(HTML.Attribute.VALUE);
                if ((key != null) && (value != null) && !(value.equals("")))
                {
                    // store the name of an input field and its value
                    map.put(key, value);
                }

            }

        }

        public void handleStartTag(HTML.Tag tag, MutableAttributeSet attr, int pos)
        {
            if (tag == HTML.Tag.OPTION)
            { // found an option-element
                if (!(attr.getAttribute(HTML.Attribute.SELECTED) == null))
                {
                    // store the name of the select-field
                    String key = lastSelect;
                    // and the value of the selected option
                    String value = (String) attr.getAttribute(HTML.Attribute.VALUE);
                    if (!(key.equals("")) && !(value.equals("")))
                    {
                        map.put(key, value);
                    }
                }
            }
            if (tag == HTML.Tag.SELECT)
            { // found a select-element
                String key = (String) attr.getAttribute(HTML.Attribute.NAME);

                if (key != null)
                {
                    lastSelect = key;
                }
            }
        }
    }

}
