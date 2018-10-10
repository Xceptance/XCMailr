package models;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@MappedSuperclass
public abstract class AbstractEntity
{
    @JsonIgnore
    @Id
    private long id;

    // -------------------------------------
    // Getters and Setters
    // -------------------------------------
    /**
     * @return the ID of this MBox (primary key in the DB)
     */
    @JsonProperty("id")
    public long getId()
    {
        return id;
    }

    /**
     * @param id
     *            the ID of this MBox to set
     */
    @JsonIgnore
    public void setId(long id)
    {
        this.id = id;
    }

    /**
     * Deletes the object from the database
     */
    public void delete()
    {
        Ebean.delete(this);
    }

    /**
     * Stores the object in the Database
     */

    public void save()
    {
        Ebean.save(this);
    }

    /**
     * Updates the object in the DB
     */
    public void update()
    {
        Ebean.update(this);
    }

}
