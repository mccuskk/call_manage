package co.kwest.www.callmanager.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import co.kwest.www.callmanager.database.entity.CGroup;

@Dao
public interface CGroupDao {

    @Insert
    long[] insert(CGroup... lists);

    @Query("DELETE FROM cgroup_table")
    int deleteAll();

    @Query("DELETE FROM cgroup_table WHERE name LIKE :name")
    int deleteByName(String name);

    @Query("DELETE FROM cgroup_table WHERE list_id LIKE :listId")
    int deleteById(long listId);

    @Query("SELECT * from cgroup_table WHERE list_id LIKE :listId")
    LiveData<List<CGroup>> getCGroupById(long listId);

    @Query("SELECT * from cgroup_table WHERE name LIKE :name")
    LiveData<List<CGroup>> getCGroupByName(String name);

    @Query("SELECT * from cgroup_table ORDER BY list_id ASC")
    LiveData<List<CGroup>> getAllCGroups();
}