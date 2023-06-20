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
package etc;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.Enumeration;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

public class HtmlUtils
{
    /**
     * Takes an HTML Document as String and parses it. It will generate the body of a page as String without the head.
     * 
     * @param result
     *            - the HTML Document as String
     * @return a String containing the pure body of the HTML-Document
     */
    public static String readHTMLData(String result)
    {
        Reader reader = new StringReader(result);
        HTMLEditorKit.Parser parser = new ParserDelegator();
        try
        {

            HTMLPageParser pageParser = new HTMLPageParser();
            parser.parse(reader, pageParser, true);
            reader.close();

            return pageParser.sb.toString();
        }
        catch (IOException e)
        {
            return "";
        }
    }

    /**
     * inner class to parse the input & selection fields
     * 
     * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
     */
    private static class HTMLPageParser extends HTMLEditorKit.ParserCallback
    {
        public StringBuilder sb = new StringBuilder();

        private boolean record = false;

        public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attr, int pos)
        {
            if (!record)
                return;

            sb.append("<");
            sb.append(tag.toString());
            sb.append(getAttributesAsString(attr));
            sb.append("/>");
        }

        public void handleStartTag(HTML.Tag tag, MutableAttributeSet attr, int pos)
        {
            if (tag == HTML.Tag.HTML || tag == HTML.Tag.BODY)
            { // found a HTML or Body-element
                record = true;
            }
            else if (tag == HTML.Tag.HEAD)
            {
                record = false;
            }
            else
            {
                sb.append("<");
                sb.append(tag.toString());
                sb.append(getAttributesAsString(attr));
                sb.append("/>");
            }
        }

        public void handleEndTag(HTML.Tag tag, int pos)
        {
            if (!record)
                return;

            sb.append("</");
            sb.append(tag.toString());
            sb.append(">");

        }

        public void handleText(char[] data, int pos)
        {
            if (!record)
                return;

            sb.append(data);
        }

        public void handleComment(char[] data, int pos)
        {
            if (!record)
                return;

            sb.append("<!-- ");
            sb.append(data);
            sb.append(" -->");
        }

        @SuppressWarnings("unchecked")
        private String getAttributesAsString(MutableAttributeSet attr)
        {
            StringBuilder sb = new StringBuilder();
            Enumeration<HTML.Attribute> attributeNames = (Enumeration<HTML.Attribute>) attr.getAttributeNames();
            for (HTML.Attribute attName : Collections.list(attributeNames))
            {
                sb.append(" ");
                sb.append(attName);
                sb.append(" = ").append("\"").append(attr.getAttribute(attName)).append("\"");
            }
            return sb.toString();
        }
    }

}
