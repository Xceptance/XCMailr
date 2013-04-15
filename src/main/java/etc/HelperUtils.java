package etc;

import java.util.HashMap;
import java.util.Map;

import ninja.utils.NinjaProperties;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class HelperUtils
{

    public static Map<String, String[]> getDomainsFromConfig(NinjaProperties ninjaProp)
    {

        String dlist = ninjaProp.get("mbox.dlist");

        if (!dlist.equals(null))
        {
            String[] list = dlist.split(";");
            if (!list.equals(null))
            {
                Map<String, String[]> map = new HashMap<String, String[]>();

                    map.put("domain", list);

                return map;
            }
        }
        return null;
    }
}
