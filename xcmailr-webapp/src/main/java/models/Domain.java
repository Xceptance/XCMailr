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
package models;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Table;

import io.ebean.Ebean;
import io.ebean.ExpressionList;

/**
 * Object to handle the allowed domains
 * 
 * @author Patrick Thum, Xceptance Software Technologies GmbH, Germany
 */
@Entity
@Table(name = "register_domains")
public class Domain extends AbstractEntity implements Serializable
{
    /** UID to serialize this object */
    private static final long serialVersionUID = 2659762572278339375L;

    /**
     * the domainname
     */
    private String domainname;

    public Domain()
    {
        this.domainname = "";
    }

    public Domain(String domainname)
    {
        this.domainname = domainname;
    }

    public String getDomainname()
    {
        return domainname;
    }

    public void setDomainname(String domainname)
    {
        this.domainname = domainname;
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
        return queryByName(name).findOne();
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
        return queryByName(name).exists();
    }

    private static ExpressionList<Domain> queryByName(final String name)
    {
        return Ebean.find(Domain.class).where().ieq("domainname", name);
    }
}
