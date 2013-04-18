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
            HTMLFormParser fp = new HTMLFormParser();
            parser.parse(reader, fp, true);
            reader.close();

            return fp.map;
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return new HashMap<String, String>();

    }
    
    /**
     * inner class for parsing the inputfields
     * @author pthum
     *
     */
    private static class HTMLFormParser extends HTMLEditorKit.ParserCallback
    {

        public Map<String, String> map = new HashMap<String, String>();

        public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos)
        {
            if (t == HTML.Tag.INPUT)
            {

                String key = (String) a.getAttribute(HTML.Attribute.NAME);
                String value = (String) a.getAttribute(HTML.Attribute.VALUE);
                if (!(key == null) && !(value.equals("")))
                {

                    map.put(key, value);
                }

            }
        }
    }

}
