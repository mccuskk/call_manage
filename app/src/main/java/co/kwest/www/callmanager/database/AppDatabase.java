package co.kwest.www.callmanager.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import co.kwest.www.callmanager.database.dao.CGroupAndItsContactsDao;
import co.kwest.www.callmanager.database.dao.CGroupDao;
import co.kwest.www.callmanager.database.dao.ContactDao;
import co.kwest.www.callmanager.database.entity.CGroup;
import co.kwest.www.callmanager.database.entity.Contact;

@Database(entities = {CGroup.class, Contact.class}, version = 3)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase sInstance;

    public static AppDatabase getDatabase(final Context context) {
        if (sInstance == null || !sInstance.isOpen()) {
            synchronized (AppDatabase.class) {
                if (sInstance == null || !sInstance.isOpen()) {
                    sInstance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "app_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return sInstance;
    }

    public abstract CGroupDao getCGroupDao();

    public abstract ContactDao getContactDao();

    public abstract CGroupAndItsContactsDao getCGroupAndItsContactsDao();
}
