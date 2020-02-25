package co.kwest.www.callmanager.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import co.kwest.www.callmanager.database.entity.CGroupAndItsContacts;

@Dao
public interface CGroupAndItsContactsDao {

    @Query("SELECT * from cgroup_table")
    @Transaction
    LiveData<List<CGroupAndItsContacts>> getAllCGroupsAndTheirContacts();
}
