/**  
 *  Copyright 2013 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.avaje.ebean.Ebean;

/**
 * Object to handle the allowed domains
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
@Entity
@Table(name = "register_domains")
public class Domain
{
    /**
     * The ID of this domain
     */
    @Id
    private long id;

    /**
     * the domainname
     */
    private String domainname;

    public Domain()
    {
        this.id = 0L;
        this.domainname = "";
    }

    public Domain(String domainname)
    {
        this.domainname = domainname;
    }

    public long getId()
    {
        return id;
    }

    public String getDomainname()
    {
        return domainname;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public void setDomainname(String domainname)
    {
        this.domainname = domainname;
    }

    /**
     * Persists the domain-object in the database
     */
    public void save()
    {
        Ebean.save(this);
    }

    /**
     * Updates the domain-object in the database
     */
    public void update()
    {
        Ebean.update(this);
    }

    /**
     * Deletes the domain-object from the database
     */
    public void delete()
    {
        Ebean.delete(this);
    }

    /**
     * Deletes the domain-object with the specified ID from the Database
     * 
     * @param id
     *            the ID of the domain
     */
    public static void delete(Long id)
    {
        Ebean.delete(Domain.class, id);
    }

    /**
     * Deletes the domain-object with the specified domain-name from the Database
     * 
     * @param name
     *            the domain-name
     */
    public static void delete(String name)
    {
        Domain domain = getByName(name);
        domain.delete();
    }

    /**
     * @param id
     *            the ID of the domain
     * @return a domain-object
     */
    public static Domain getById(Long id)
    {
        return Ebean.find(Domain.class, id);
    }

    /**
     * @param name
     *            the domain-name
     * @return the domain-object for this name
     */
    public static Domain getByName(String name)
    {
        return Ebean.find(Domain.class).where().eq("domainname", name).findUnique();
    }

    /**
     * @return all listed domains in the database
     */
    public static List<Domain> getAll()
    {
        return Ebean.find(Domain.class).findList();
    }

    /**
     * Indicates whether a domain-name already exists
     * 
     * @param name
     *            the domain-name to search for
     * @return true, if the specified name already exists
     */
    public static boolean exists(String name)
    {
        return !Ebean.find(Domain.class).where().eq("domainname", name).findList().isEmpty();
    }
}
