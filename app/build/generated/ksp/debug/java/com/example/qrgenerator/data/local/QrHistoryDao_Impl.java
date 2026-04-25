package com.example.qrgenerator.data.local;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class QrHistoryDao_Impl implements QrHistoryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<QrHistoryEntity> __insertionAdapterOfQrHistoryEntity;

  private final EntityDeletionOrUpdateAdapter<QrHistoryEntity> __deletionAdapterOfQrHistoryEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public QrHistoryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfQrHistoryEntity = new EntityInsertionAdapter<QrHistoryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `qr_history` (`id`,`configJson`,`thumbnailPath`,`contentType`,`contentPreview`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final QrHistoryEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getConfigJson());
        statement.bindString(3, entity.getThumbnailPath());
        statement.bindString(4, entity.getContentType());
        statement.bindString(5, entity.getContentPreview());
        statement.bindLong(6, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfQrHistoryEntity = new EntityDeletionOrUpdateAdapter<QrHistoryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `qr_history` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final QrHistoryEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM qr_history";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final QrHistoryEntity entity, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfQrHistoryEntity.insertAndReturnId(entity);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final QrHistoryEntity entity, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfQrHistoryEntity.handle(entity);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<QrHistoryEntity>> getAllHistory() {
    final String _sql = "SELECT * FROM qr_history ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"qr_history"}, new Callable<List<QrHistoryEntity>>() {
      @Override
      @NonNull
      public List<QrHistoryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfConfigJson = CursorUtil.getColumnIndexOrThrow(_cursor, "configJson");
          final int _cursorIndexOfThumbnailPath = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnailPath");
          final int _cursorIndexOfContentType = CursorUtil.getColumnIndexOrThrow(_cursor, "contentType");
          final int _cursorIndexOfContentPreview = CursorUtil.getColumnIndexOrThrow(_cursor, "contentPreview");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<QrHistoryEntity> _result = new ArrayList<QrHistoryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final QrHistoryEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpConfigJson;
            _tmpConfigJson = _cursor.getString(_cursorIndexOfConfigJson);
            final String _tmpThumbnailPath;
            _tmpThumbnailPath = _cursor.getString(_cursorIndexOfThumbnailPath);
            final String _tmpContentType;
            _tmpContentType = _cursor.getString(_cursorIndexOfContentType);
            final String _tmpContentPreview;
            _tmpContentPreview = _cursor.getString(_cursorIndexOfContentPreview);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new QrHistoryEntity(_tmpId,_tmpConfigJson,_tmpThumbnailPath,_tmpContentType,_tmpContentPreview,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<QrHistoryEntity>> getHistoryByType(final String type) {
    final String _sql = "SELECT * FROM qr_history WHERE contentType = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, type);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"qr_history"}, new Callable<List<QrHistoryEntity>>() {
      @Override
      @NonNull
      public List<QrHistoryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfConfigJson = CursorUtil.getColumnIndexOrThrow(_cursor, "configJson");
          final int _cursorIndexOfThumbnailPath = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnailPath");
          final int _cursorIndexOfContentType = CursorUtil.getColumnIndexOrThrow(_cursor, "contentType");
          final int _cursorIndexOfContentPreview = CursorUtil.getColumnIndexOrThrow(_cursor, "contentPreview");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<QrHistoryEntity> _result = new ArrayList<QrHistoryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final QrHistoryEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpConfigJson;
            _tmpConfigJson = _cursor.getString(_cursorIndexOfConfigJson);
            final String _tmpThumbnailPath;
            _tmpThumbnailPath = _cursor.getString(_cursorIndexOfThumbnailPath);
            final String _tmpContentType;
            _tmpContentType = _cursor.getString(_cursorIndexOfContentType);
            final String _tmpContentPreview;
            _tmpContentPreview = _cursor.getString(_cursorIndexOfContentPreview);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new QrHistoryEntity(_tmpId,_tmpConfigJson,_tmpThumbnailPath,_tmpContentType,_tmpContentPreview,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<QrHistoryEntity>> searchHistory(final String query) {
    final String _sql = "SELECT * FROM qr_history WHERE contentPreview LIKE '%' || ? || '%' ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, query);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"qr_history"}, new Callable<List<QrHistoryEntity>>() {
      @Override
      @NonNull
      public List<QrHistoryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfConfigJson = CursorUtil.getColumnIndexOrThrow(_cursor, "configJson");
          final int _cursorIndexOfThumbnailPath = CursorUtil.getColumnIndexOrThrow(_cursor, "thumbnailPath");
          final int _cursorIndexOfContentType = CursorUtil.getColumnIndexOrThrow(_cursor, "contentType");
          final int _cursorIndexOfContentPreview = CursorUtil.getColumnIndexOrThrow(_cursor, "contentPreview");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<QrHistoryEntity> _result = new ArrayList<QrHistoryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final QrHistoryEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpConfigJson;
            _tmpConfigJson = _cursor.getString(_cursorIndexOfConfigJson);
            final String _tmpThumbnailPath;
            _tmpThumbnailPath = _cursor.getString(_cursorIndexOfThumbnailPath);
            final String _tmpContentType;
            _tmpContentType = _cursor.getString(_cursorIndexOfContentType);
            final String _tmpContentPreview;
            _tmpContentPreview = _cursor.getString(_cursorIndexOfContentPreview);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new QrHistoryEntity(_tmpId,_tmpConfigJson,_tmpThumbnailPath,_tmpContentType,_tmpContentPreview,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Integer> getCount() {
    final String _sql = "SELECT COUNT(*) FROM qr_history";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"qr_history"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
