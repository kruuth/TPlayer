package com.grok.tplayer.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomDatabaseKt;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.grok.tplayer.data.model.AlbumArt;
import com.grok.tplayer.data.model.SourceFolder;
import com.grok.tplayer.data.model.Track;
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
public final class TrackDao_Impl implements TrackDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Track> __insertionAdapterOfTrack;

  private final EntityInsertionAdapter<AlbumArt> __insertionAdapterOfAlbumArt;

  private final EntityInsertionAdapter<SourceFolder> __insertionAdapterOfSourceFolder;

  private final SharedSQLiteStatement __preparedStmtOfClearAllTracks;

  private final SharedSQLiteStatement __preparedStmtOfDeleteTracksUnderFolder;

  private final SharedSQLiteStatement __preparedStmtOfDeleteArtsForTrack;

  private final SharedSQLiteStatement __preparedStmtOfClearSources;

  public TrackDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTrack = new EntityInsertionAdapter<Track>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `tracks` (`id`,`uri`,`fileName`,`title`,`artist`,`album`,`year`,`trackNumber`,`composer`,`comment`,`durationMs`,`folderPath`,`albumArtPath`,`hasMultipleArts`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Track entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getUri());
        statement.bindString(3, entity.getFileName());
        if (entity.getTitle() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getTitle());
        }
        if (entity.getArtist() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getArtist());
        }
        if (entity.getAlbum() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getAlbum());
        }
        if (entity.getYear() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getYear());
        }
        if (entity.getTrackNumber() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getTrackNumber());
        }
        if (entity.getComposer() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getComposer());
        }
        if (entity.getComment() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getComment());
        }
        statement.bindLong(11, entity.getDurationMs());
        statement.bindString(12, entity.getFolderPath());
        if (entity.getAlbumArtPath() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getAlbumArtPath());
        }
        final int _tmp = entity.getHasMultipleArts() ? 1 : 0;
        statement.bindLong(14, _tmp);
      }
    };
    this.__insertionAdapterOfAlbumArt = new EntityInsertionAdapter<AlbumArt>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `album_arts` (`id`,`trackId`,`artPath`,`pictureType`,`isPrimary`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AlbumArt entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTrackId());
        statement.bindString(3, entity.getArtPath());
        statement.bindLong(4, entity.getPictureType());
        final int _tmp = entity.isPrimary() ? 1 : 0;
        statement.bindLong(5, _tmp);
      }
    };
    this.__insertionAdapterOfSourceFolder = new EntityInsertionAdapter<SourceFolder>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `source_folders` (`uri`,`displayName`,`isDefault`,`lastScanned`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SourceFolder entity) {
        statement.bindString(1, entity.getUri());
        statement.bindString(2, entity.getDisplayName());
        final int _tmp = entity.isDefault() ? 1 : 0;
        statement.bindLong(3, _tmp);
        statement.bindLong(4, entity.getLastScanned());
      }
    };
    this.__preparedStmtOfClearAllTracks = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM tracks";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteTracksUnderFolder = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM tracks WHERE folderPath = ? OR folderPath LIKE ? || '/%'";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteArtsForTrack = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM album_arts WHERE trackId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearSources = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM source_folders";
        return _query;
      }
    };
  }

  @Override
  public Object insertTrack(final Track track, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfTrack.insertAndReturnId(track);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertTracks(final List<Track> tracks,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTrack.insert(tracks);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAlbumArt(final AlbumArt art, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfAlbumArt.insertAndReturnId(art);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAlbumArts(final List<AlbumArt> arts,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAlbumArt.insert(arts);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertSource(final SourceFolder source,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSourceFolder.insert(source);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object replaceLibrary(final List<Track> tracks, final List<AlbumArt> arts,
      final Continuation<? super Unit> $completion) {
    return RoomDatabaseKt.withTransaction(__db, (__cont) -> TrackDao.DefaultImpls.replaceLibrary(TrackDao_Impl.this, tracks, arts, __cont), $completion);
  }

  @Override
  public Object clearAllTracks(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAllTracks.acquire();
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
          __preparedStmtOfClearAllTracks.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteTracksUnderFolder(final String prefix,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteTracksUnderFolder.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, prefix);
        _argIndex = 2;
        _stmt.bindString(_argIndex, prefix);
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
          __preparedStmtOfDeleteTracksUnderFolder.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteArtsForTrack(final long trackId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteArtsForTrack.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, trackId);
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
          __preparedStmtOfDeleteArtsForTrack.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearSources(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearSources.acquire();
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
          __preparedStmtOfClearSources.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Track>> getAllTracks() {
    final String _sql = "SELECT * FROM tracks ORDER BY title COLLATE NOCASE ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tracks"}, new Callable<List<Track>>() {
      @Override
      @NonNull
      public List<Track> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUri = CursorUtil.getColumnIndexOrThrow(_cursor, "uri");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfArtist = CursorUtil.getColumnIndexOrThrow(_cursor, "artist");
          final int _cursorIndexOfAlbum = CursorUtil.getColumnIndexOrThrow(_cursor, "album");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfTrackNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "trackNumber");
          final int _cursorIndexOfComposer = CursorUtil.getColumnIndexOrThrow(_cursor, "composer");
          final int _cursorIndexOfComment = CursorUtil.getColumnIndexOrThrow(_cursor, "comment");
          final int _cursorIndexOfDurationMs = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMs");
          final int _cursorIndexOfFolderPath = CursorUtil.getColumnIndexOrThrow(_cursor, "folderPath");
          final int _cursorIndexOfAlbumArtPath = CursorUtil.getColumnIndexOrThrow(_cursor, "albumArtPath");
          final int _cursorIndexOfHasMultipleArts = CursorUtil.getColumnIndexOrThrow(_cursor, "hasMultipleArts");
          final List<Track> _result = new ArrayList<Track>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Track _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUri;
            _tmpUri = _cursor.getString(_cursorIndexOfUri);
            final String _tmpFileName;
            _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpArtist;
            if (_cursor.isNull(_cursorIndexOfArtist)) {
              _tmpArtist = null;
            } else {
              _tmpArtist = _cursor.getString(_cursorIndexOfArtist);
            }
            final String _tmpAlbum;
            if (_cursor.isNull(_cursorIndexOfAlbum)) {
              _tmpAlbum = null;
            } else {
              _tmpAlbum = _cursor.getString(_cursorIndexOfAlbum);
            }
            final Integer _tmpYear;
            if (_cursor.isNull(_cursorIndexOfYear)) {
              _tmpYear = null;
            } else {
              _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            }
            final Integer _tmpTrackNumber;
            if (_cursor.isNull(_cursorIndexOfTrackNumber)) {
              _tmpTrackNumber = null;
            } else {
              _tmpTrackNumber = _cursor.getInt(_cursorIndexOfTrackNumber);
            }
            final String _tmpComposer;
            if (_cursor.isNull(_cursorIndexOfComposer)) {
              _tmpComposer = null;
            } else {
              _tmpComposer = _cursor.getString(_cursorIndexOfComposer);
            }
            final String _tmpComment;
            if (_cursor.isNull(_cursorIndexOfComment)) {
              _tmpComment = null;
            } else {
              _tmpComment = _cursor.getString(_cursorIndexOfComment);
            }
            final long _tmpDurationMs;
            _tmpDurationMs = _cursor.getLong(_cursorIndexOfDurationMs);
            final String _tmpFolderPath;
            _tmpFolderPath = _cursor.getString(_cursorIndexOfFolderPath);
            final String _tmpAlbumArtPath;
            if (_cursor.isNull(_cursorIndexOfAlbumArtPath)) {
              _tmpAlbumArtPath = null;
            } else {
              _tmpAlbumArtPath = _cursor.getString(_cursorIndexOfAlbumArtPath);
            }
            final boolean _tmpHasMultipleArts;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasMultipleArts);
            _tmpHasMultipleArts = _tmp != 0;
            _item = new Track(_tmpId,_tmpUri,_tmpFileName,_tmpTitle,_tmpArtist,_tmpAlbum,_tmpYear,_tmpTrackNumber,_tmpComposer,_tmpComment,_tmpDurationMs,_tmpFolderPath,_tmpAlbumArtPath,_tmpHasMultipleArts);
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
  public Object getTrackById(final long id, final Continuation<? super Track> $completion) {
    final String _sql = "SELECT * FROM tracks WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Track>() {
      @Override
      @Nullable
      public Track call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUri = CursorUtil.getColumnIndexOrThrow(_cursor, "uri");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfArtist = CursorUtil.getColumnIndexOrThrow(_cursor, "artist");
          final int _cursorIndexOfAlbum = CursorUtil.getColumnIndexOrThrow(_cursor, "album");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfTrackNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "trackNumber");
          final int _cursorIndexOfComposer = CursorUtil.getColumnIndexOrThrow(_cursor, "composer");
          final int _cursorIndexOfComment = CursorUtil.getColumnIndexOrThrow(_cursor, "comment");
          final int _cursorIndexOfDurationMs = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMs");
          final int _cursorIndexOfFolderPath = CursorUtil.getColumnIndexOrThrow(_cursor, "folderPath");
          final int _cursorIndexOfAlbumArtPath = CursorUtil.getColumnIndexOrThrow(_cursor, "albumArtPath");
          final int _cursorIndexOfHasMultipleArts = CursorUtil.getColumnIndexOrThrow(_cursor, "hasMultipleArts");
          final Track _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUri;
            _tmpUri = _cursor.getString(_cursorIndexOfUri);
            final String _tmpFileName;
            _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpArtist;
            if (_cursor.isNull(_cursorIndexOfArtist)) {
              _tmpArtist = null;
            } else {
              _tmpArtist = _cursor.getString(_cursorIndexOfArtist);
            }
            final String _tmpAlbum;
            if (_cursor.isNull(_cursorIndexOfAlbum)) {
              _tmpAlbum = null;
            } else {
              _tmpAlbum = _cursor.getString(_cursorIndexOfAlbum);
            }
            final Integer _tmpYear;
            if (_cursor.isNull(_cursorIndexOfYear)) {
              _tmpYear = null;
            } else {
              _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            }
            final Integer _tmpTrackNumber;
            if (_cursor.isNull(_cursorIndexOfTrackNumber)) {
              _tmpTrackNumber = null;
            } else {
              _tmpTrackNumber = _cursor.getInt(_cursorIndexOfTrackNumber);
            }
            final String _tmpComposer;
            if (_cursor.isNull(_cursorIndexOfComposer)) {
              _tmpComposer = null;
            } else {
              _tmpComposer = _cursor.getString(_cursorIndexOfComposer);
            }
            final String _tmpComment;
            if (_cursor.isNull(_cursorIndexOfComment)) {
              _tmpComment = null;
            } else {
              _tmpComment = _cursor.getString(_cursorIndexOfComment);
            }
            final long _tmpDurationMs;
            _tmpDurationMs = _cursor.getLong(_cursorIndexOfDurationMs);
            final String _tmpFolderPath;
            _tmpFolderPath = _cursor.getString(_cursorIndexOfFolderPath);
            final String _tmpAlbumArtPath;
            if (_cursor.isNull(_cursorIndexOfAlbumArtPath)) {
              _tmpAlbumArtPath = null;
            } else {
              _tmpAlbumArtPath = _cursor.getString(_cursorIndexOfAlbumArtPath);
            }
            final boolean _tmpHasMultipleArts;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasMultipleArts);
            _tmpHasMultipleArts = _tmp != 0;
            _result = new Track(_tmpId,_tmpUri,_tmpFileName,_tmpTitle,_tmpArtist,_tmpAlbum,_tmpYear,_tmpTrackNumber,_tmpComposer,_tmpComment,_tmpDurationMs,_tmpFolderPath,_tmpAlbumArtPath,_tmpHasMultipleArts);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Track>> searchTracks(final String q) {
    final String _sql = "\n"
            + "        SELECT * FROM tracks WHERE\n"
            + "            title LIKE '%' || ? || '%' COLLATE NOCASE OR\n"
            + "            artist LIKE '%' || ? || '%' COLLATE NOCASE OR\n"
            + "            album LIKE '%' || ? || '%' COLLATE NOCASE OR\n"
            + "            fileName LIKE '%' || ? || '%' COLLATE NOCASE OR\n"
            + "            folderPath LIKE '%' || ? || '%' COLLATE NOCASE\n"
            + "        ORDER BY title COLLATE NOCASE ASC\n"
            + "        LIMIT 200\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 5);
    int _argIndex = 1;
    _statement.bindString(_argIndex, q);
    _argIndex = 2;
    _statement.bindString(_argIndex, q);
    _argIndex = 3;
    _statement.bindString(_argIndex, q);
    _argIndex = 4;
    _statement.bindString(_argIndex, q);
    _argIndex = 5;
    _statement.bindString(_argIndex, q);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tracks"}, new Callable<List<Track>>() {
      @Override
      @NonNull
      public List<Track> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUri = CursorUtil.getColumnIndexOrThrow(_cursor, "uri");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfArtist = CursorUtil.getColumnIndexOrThrow(_cursor, "artist");
          final int _cursorIndexOfAlbum = CursorUtil.getColumnIndexOrThrow(_cursor, "album");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfTrackNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "trackNumber");
          final int _cursorIndexOfComposer = CursorUtil.getColumnIndexOrThrow(_cursor, "composer");
          final int _cursorIndexOfComment = CursorUtil.getColumnIndexOrThrow(_cursor, "comment");
          final int _cursorIndexOfDurationMs = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMs");
          final int _cursorIndexOfFolderPath = CursorUtil.getColumnIndexOrThrow(_cursor, "folderPath");
          final int _cursorIndexOfAlbumArtPath = CursorUtil.getColumnIndexOrThrow(_cursor, "albumArtPath");
          final int _cursorIndexOfHasMultipleArts = CursorUtil.getColumnIndexOrThrow(_cursor, "hasMultipleArts");
          final List<Track> _result = new ArrayList<Track>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Track _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUri;
            _tmpUri = _cursor.getString(_cursorIndexOfUri);
            final String _tmpFileName;
            _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpArtist;
            if (_cursor.isNull(_cursorIndexOfArtist)) {
              _tmpArtist = null;
            } else {
              _tmpArtist = _cursor.getString(_cursorIndexOfArtist);
            }
            final String _tmpAlbum;
            if (_cursor.isNull(_cursorIndexOfAlbum)) {
              _tmpAlbum = null;
            } else {
              _tmpAlbum = _cursor.getString(_cursorIndexOfAlbum);
            }
            final Integer _tmpYear;
            if (_cursor.isNull(_cursorIndexOfYear)) {
              _tmpYear = null;
            } else {
              _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            }
            final Integer _tmpTrackNumber;
            if (_cursor.isNull(_cursorIndexOfTrackNumber)) {
              _tmpTrackNumber = null;
            } else {
              _tmpTrackNumber = _cursor.getInt(_cursorIndexOfTrackNumber);
            }
            final String _tmpComposer;
            if (_cursor.isNull(_cursorIndexOfComposer)) {
              _tmpComposer = null;
            } else {
              _tmpComposer = _cursor.getString(_cursorIndexOfComposer);
            }
            final String _tmpComment;
            if (_cursor.isNull(_cursorIndexOfComment)) {
              _tmpComment = null;
            } else {
              _tmpComment = _cursor.getString(_cursorIndexOfComment);
            }
            final long _tmpDurationMs;
            _tmpDurationMs = _cursor.getLong(_cursorIndexOfDurationMs);
            final String _tmpFolderPath;
            _tmpFolderPath = _cursor.getString(_cursorIndexOfFolderPath);
            final String _tmpAlbumArtPath;
            if (_cursor.isNull(_cursorIndexOfAlbumArtPath)) {
              _tmpAlbumArtPath = null;
            } else {
              _tmpAlbumArtPath = _cursor.getString(_cursorIndexOfAlbumArtPath);
            }
            final boolean _tmpHasMultipleArts;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasMultipleArts);
            _tmpHasMultipleArts = _tmp != 0;
            _item = new Track(_tmpId,_tmpUri,_tmpFileName,_tmpTitle,_tmpArtist,_tmpAlbum,_tmpYear,_tmpTrackNumber,_tmpComposer,_tmpComment,_tmpDurationMs,_tmpFolderPath,_tmpAlbumArtPath,_tmpHasMultipleArts);
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
  public Object getAllTracksList(final Continuation<? super List<Track>> $completion) {
    final String _sql = "SELECT * FROM tracks ORDER BY title COLLATE NOCASE ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Track>>() {
      @Override
      @NonNull
      public List<Track> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUri = CursorUtil.getColumnIndexOrThrow(_cursor, "uri");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfArtist = CursorUtil.getColumnIndexOrThrow(_cursor, "artist");
          final int _cursorIndexOfAlbum = CursorUtil.getColumnIndexOrThrow(_cursor, "album");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfTrackNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "trackNumber");
          final int _cursorIndexOfComposer = CursorUtil.getColumnIndexOrThrow(_cursor, "composer");
          final int _cursorIndexOfComment = CursorUtil.getColumnIndexOrThrow(_cursor, "comment");
          final int _cursorIndexOfDurationMs = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMs");
          final int _cursorIndexOfFolderPath = CursorUtil.getColumnIndexOrThrow(_cursor, "folderPath");
          final int _cursorIndexOfAlbumArtPath = CursorUtil.getColumnIndexOrThrow(_cursor, "albumArtPath");
          final int _cursorIndexOfHasMultipleArts = CursorUtil.getColumnIndexOrThrow(_cursor, "hasMultipleArts");
          final List<Track> _result = new ArrayList<Track>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Track _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUri;
            _tmpUri = _cursor.getString(_cursorIndexOfUri);
            final String _tmpFileName;
            _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpArtist;
            if (_cursor.isNull(_cursorIndexOfArtist)) {
              _tmpArtist = null;
            } else {
              _tmpArtist = _cursor.getString(_cursorIndexOfArtist);
            }
            final String _tmpAlbum;
            if (_cursor.isNull(_cursorIndexOfAlbum)) {
              _tmpAlbum = null;
            } else {
              _tmpAlbum = _cursor.getString(_cursorIndexOfAlbum);
            }
            final Integer _tmpYear;
            if (_cursor.isNull(_cursorIndexOfYear)) {
              _tmpYear = null;
            } else {
              _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            }
            final Integer _tmpTrackNumber;
            if (_cursor.isNull(_cursorIndexOfTrackNumber)) {
              _tmpTrackNumber = null;
            } else {
              _tmpTrackNumber = _cursor.getInt(_cursorIndexOfTrackNumber);
            }
            final String _tmpComposer;
            if (_cursor.isNull(_cursorIndexOfComposer)) {
              _tmpComposer = null;
            } else {
              _tmpComposer = _cursor.getString(_cursorIndexOfComposer);
            }
            final String _tmpComment;
            if (_cursor.isNull(_cursorIndexOfComment)) {
              _tmpComment = null;
            } else {
              _tmpComment = _cursor.getString(_cursorIndexOfComment);
            }
            final long _tmpDurationMs;
            _tmpDurationMs = _cursor.getLong(_cursorIndexOfDurationMs);
            final String _tmpFolderPath;
            _tmpFolderPath = _cursor.getString(_cursorIndexOfFolderPath);
            final String _tmpAlbumArtPath;
            if (_cursor.isNull(_cursorIndexOfAlbumArtPath)) {
              _tmpAlbumArtPath = null;
            } else {
              _tmpAlbumArtPath = _cursor.getString(_cursorIndexOfAlbumArtPath);
            }
            final boolean _tmpHasMultipleArts;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasMultipleArts);
            _tmpHasMultipleArts = _tmp != 0;
            _item = new Track(_tmpId,_tmpUri,_tmpFileName,_tmpTitle,_tmpArtist,_tmpAlbum,_tmpYear,_tmpTrackNumber,_tmpComposer,_tmpComment,_tmpDurationMs,_tmpFolderPath,_tmpAlbumArtPath,_tmpHasMultipleArts);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<String>> getAllArtists() {
    final String _sql = "SELECT DISTINCT artist FROM tracks WHERE artist IS NOT NULL AND artist != '' ORDER BY artist COLLATE NOCASE ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tracks"}, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
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
  public Flow<List<Track>> getTracksByArtist(final String artist) {
    final String _sql = "SELECT * FROM tracks WHERE artist = ? ORDER BY album COLLATE NOCASE ASC, trackNumber ASC, title COLLATE NOCASE ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, artist);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tracks"}, new Callable<List<Track>>() {
      @Override
      @NonNull
      public List<Track> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUri = CursorUtil.getColumnIndexOrThrow(_cursor, "uri");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfArtist = CursorUtil.getColumnIndexOrThrow(_cursor, "artist");
          final int _cursorIndexOfAlbum = CursorUtil.getColumnIndexOrThrow(_cursor, "album");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfTrackNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "trackNumber");
          final int _cursorIndexOfComposer = CursorUtil.getColumnIndexOrThrow(_cursor, "composer");
          final int _cursorIndexOfComment = CursorUtil.getColumnIndexOrThrow(_cursor, "comment");
          final int _cursorIndexOfDurationMs = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMs");
          final int _cursorIndexOfFolderPath = CursorUtil.getColumnIndexOrThrow(_cursor, "folderPath");
          final int _cursorIndexOfAlbumArtPath = CursorUtil.getColumnIndexOrThrow(_cursor, "albumArtPath");
          final int _cursorIndexOfHasMultipleArts = CursorUtil.getColumnIndexOrThrow(_cursor, "hasMultipleArts");
          final List<Track> _result = new ArrayList<Track>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Track _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUri;
            _tmpUri = _cursor.getString(_cursorIndexOfUri);
            final String _tmpFileName;
            _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpArtist;
            if (_cursor.isNull(_cursorIndexOfArtist)) {
              _tmpArtist = null;
            } else {
              _tmpArtist = _cursor.getString(_cursorIndexOfArtist);
            }
            final String _tmpAlbum;
            if (_cursor.isNull(_cursorIndexOfAlbum)) {
              _tmpAlbum = null;
            } else {
              _tmpAlbum = _cursor.getString(_cursorIndexOfAlbum);
            }
            final Integer _tmpYear;
            if (_cursor.isNull(_cursorIndexOfYear)) {
              _tmpYear = null;
            } else {
              _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            }
            final Integer _tmpTrackNumber;
            if (_cursor.isNull(_cursorIndexOfTrackNumber)) {
              _tmpTrackNumber = null;
            } else {
              _tmpTrackNumber = _cursor.getInt(_cursorIndexOfTrackNumber);
            }
            final String _tmpComposer;
            if (_cursor.isNull(_cursorIndexOfComposer)) {
              _tmpComposer = null;
            } else {
              _tmpComposer = _cursor.getString(_cursorIndexOfComposer);
            }
            final String _tmpComment;
            if (_cursor.isNull(_cursorIndexOfComment)) {
              _tmpComment = null;
            } else {
              _tmpComment = _cursor.getString(_cursorIndexOfComment);
            }
            final long _tmpDurationMs;
            _tmpDurationMs = _cursor.getLong(_cursorIndexOfDurationMs);
            final String _tmpFolderPath;
            _tmpFolderPath = _cursor.getString(_cursorIndexOfFolderPath);
            final String _tmpAlbumArtPath;
            if (_cursor.isNull(_cursorIndexOfAlbumArtPath)) {
              _tmpAlbumArtPath = null;
            } else {
              _tmpAlbumArtPath = _cursor.getString(_cursorIndexOfAlbumArtPath);
            }
            final boolean _tmpHasMultipleArts;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasMultipleArts);
            _tmpHasMultipleArts = _tmp != 0;
            _item = new Track(_tmpId,_tmpUri,_tmpFileName,_tmpTitle,_tmpArtist,_tmpAlbum,_tmpYear,_tmpTrackNumber,_tmpComposer,_tmpComment,_tmpDurationMs,_tmpFolderPath,_tmpAlbumArtPath,_tmpHasMultipleArts);
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
  public Flow<List<String>> getAlbumsByArtist(final String artist) {
    final String _sql = "SELECT DISTINCT album FROM tracks WHERE artist = ? AND album IS NOT NULL ORDER BY album COLLATE NOCASE ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, artist);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tracks"}, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
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
  public Flow<List<Track>> getTracksByAlbum(final String artist, final String album) {
    final String _sql = "SELECT * FROM tracks WHERE album = ? AND artist = ? ORDER BY trackNumber ASC, title COLLATE NOCASE ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, album);
    _argIndex = 2;
    _statement.bindString(_argIndex, artist);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tracks"}, new Callable<List<Track>>() {
      @Override
      @NonNull
      public List<Track> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUri = CursorUtil.getColumnIndexOrThrow(_cursor, "uri");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfArtist = CursorUtil.getColumnIndexOrThrow(_cursor, "artist");
          final int _cursorIndexOfAlbum = CursorUtil.getColumnIndexOrThrow(_cursor, "album");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfTrackNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "trackNumber");
          final int _cursorIndexOfComposer = CursorUtil.getColumnIndexOrThrow(_cursor, "composer");
          final int _cursorIndexOfComment = CursorUtil.getColumnIndexOrThrow(_cursor, "comment");
          final int _cursorIndexOfDurationMs = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMs");
          final int _cursorIndexOfFolderPath = CursorUtil.getColumnIndexOrThrow(_cursor, "folderPath");
          final int _cursorIndexOfAlbumArtPath = CursorUtil.getColumnIndexOrThrow(_cursor, "albumArtPath");
          final int _cursorIndexOfHasMultipleArts = CursorUtil.getColumnIndexOrThrow(_cursor, "hasMultipleArts");
          final List<Track> _result = new ArrayList<Track>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Track _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUri;
            _tmpUri = _cursor.getString(_cursorIndexOfUri);
            final String _tmpFileName;
            _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpArtist;
            if (_cursor.isNull(_cursorIndexOfArtist)) {
              _tmpArtist = null;
            } else {
              _tmpArtist = _cursor.getString(_cursorIndexOfArtist);
            }
            final String _tmpAlbum;
            if (_cursor.isNull(_cursorIndexOfAlbum)) {
              _tmpAlbum = null;
            } else {
              _tmpAlbum = _cursor.getString(_cursorIndexOfAlbum);
            }
            final Integer _tmpYear;
            if (_cursor.isNull(_cursorIndexOfYear)) {
              _tmpYear = null;
            } else {
              _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            }
            final Integer _tmpTrackNumber;
            if (_cursor.isNull(_cursorIndexOfTrackNumber)) {
              _tmpTrackNumber = null;
            } else {
              _tmpTrackNumber = _cursor.getInt(_cursorIndexOfTrackNumber);
            }
            final String _tmpComposer;
            if (_cursor.isNull(_cursorIndexOfComposer)) {
              _tmpComposer = null;
            } else {
              _tmpComposer = _cursor.getString(_cursorIndexOfComposer);
            }
            final String _tmpComment;
            if (_cursor.isNull(_cursorIndexOfComment)) {
              _tmpComment = null;
            } else {
              _tmpComment = _cursor.getString(_cursorIndexOfComment);
            }
            final long _tmpDurationMs;
            _tmpDurationMs = _cursor.getLong(_cursorIndexOfDurationMs);
            final String _tmpFolderPath;
            _tmpFolderPath = _cursor.getString(_cursorIndexOfFolderPath);
            final String _tmpAlbumArtPath;
            if (_cursor.isNull(_cursorIndexOfAlbumArtPath)) {
              _tmpAlbumArtPath = null;
            } else {
              _tmpAlbumArtPath = _cursor.getString(_cursorIndexOfAlbumArtPath);
            }
            final boolean _tmpHasMultipleArts;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasMultipleArts);
            _tmpHasMultipleArts = _tmp != 0;
            _item = new Track(_tmpId,_tmpUri,_tmpFileName,_tmpTitle,_tmpArtist,_tmpAlbum,_tmpYear,_tmpTrackNumber,_tmpComposer,_tmpComment,_tmpDurationMs,_tmpFolderPath,_tmpAlbumArtPath,_tmpHasMultipleArts);
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
  public Flow<List<Integer>> getAllYears() {
    final String _sql = "SELECT DISTINCT year FROM tracks WHERE year IS NOT NULL ORDER BY year DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tracks"}, new Callable<List<Integer>>() {
      @Override
      @NonNull
      public List<Integer> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<Integer> _result = new ArrayList<Integer>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Integer _item;
            _item = _cursor.getInt(0);
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
  public Flow<List<Track>> getTracksByYear(final int year) {
    final String _sql = "SELECT * FROM tracks WHERE year = ? ORDER BY artist COLLATE NOCASE ASC, album COLLATE NOCASE ASC, trackNumber ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, year);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tracks"}, new Callable<List<Track>>() {
      @Override
      @NonNull
      public List<Track> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUri = CursorUtil.getColumnIndexOrThrow(_cursor, "uri");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfArtist = CursorUtil.getColumnIndexOrThrow(_cursor, "artist");
          final int _cursorIndexOfAlbum = CursorUtil.getColumnIndexOrThrow(_cursor, "album");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfTrackNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "trackNumber");
          final int _cursorIndexOfComposer = CursorUtil.getColumnIndexOrThrow(_cursor, "composer");
          final int _cursorIndexOfComment = CursorUtil.getColumnIndexOrThrow(_cursor, "comment");
          final int _cursorIndexOfDurationMs = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMs");
          final int _cursorIndexOfFolderPath = CursorUtil.getColumnIndexOrThrow(_cursor, "folderPath");
          final int _cursorIndexOfAlbumArtPath = CursorUtil.getColumnIndexOrThrow(_cursor, "albumArtPath");
          final int _cursorIndexOfHasMultipleArts = CursorUtil.getColumnIndexOrThrow(_cursor, "hasMultipleArts");
          final List<Track> _result = new ArrayList<Track>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Track _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUri;
            _tmpUri = _cursor.getString(_cursorIndexOfUri);
            final String _tmpFileName;
            _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpArtist;
            if (_cursor.isNull(_cursorIndexOfArtist)) {
              _tmpArtist = null;
            } else {
              _tmpArtist = _cursor.getString(_cursorIndexOfArtist);
            }
            final String _tmpAlbum;
            if (_cursor.isNull(_cursorIndexOfAlbum)) {
              _tmpAlbum = null;
            } else {
              _tmpAlbum = _cursor.getString(_cursorIndexOfAlbum);
            }
            final Integer _tmpYear;
            if (_cursor.isNull(_cursorIndexOfYear)) {
              _tmpYear = null;
            } else {
              _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            }
            final Integer _tmpTrackNumber;
            if (_cursor.isNull(_cursorIndexOfTrackNumber)) {
              _tmpTrackNumber = null;
            } else {
              _tmpTrackNumber = _cursor.getInt(_cursorIndexOfTrackNumber);
            }
            final String _tmpComposer;
            if (_cursor.isNull(_cursorIndexOfComposer)) {
              _tmpComposer = null;
            } else {
              _tmpComposer = _cursor.getString(_cursorIndexOfComposer);
            }
            final String _tmpComment;
            if (_cursor.isNull(_cursorIndexOfComment)) {
              _tmpComment = null;
            } else {
              _tmpComment = _cursor.getString(_cursorIndexOfComment);
            }
            final long _tmpDurationMs;
            _tmpDurationMs = _cursor.getLong(_cursorIndexOfDurationMs);
            final String _tmpFolderPath;
            _tmpFolderPath = _cursor.getString(_cursorIndexOfFolderPath);
            final String _tmpAlbumArtPath;
            if (_cursor.isNull(_cursorIndexOfAlbumArtPath)) {
              _tmpAlbumArtPath = null;
            } else {
              _tmpAlbumArtPath = _cursor.getString(_cursorIndexOfAlbumArtPath);
            }
            final boolean _tmpHasMultipleArts;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasMultipleArts);
            _tmpHasMultipleArts = _tmp != 0;
            _item = new Track(_tmpId,_tmpUri,_tmpFileName,_tmpTitle,_tmpArtist,_tmpAlbum,_tmpYear,_tmpTrackNumber,_tmpComposer,_tmpComment,_tmpDurationMs,_tmpFolderPath,_tmpAlbumArtPath,_tmpHasMultipleArts);
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
  public Flow<List<String>> getAllFolders() {
    final String _sql = "SELECT DISTINCT folderPath FROM tracks ORDER BY folderPath COLLATE NOCASE ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tracks"}, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
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
  public Flow<List<Track>> getTracksByFolder(final String folder) {
    final String _sql = "SELECT * FROM tracks WHERE folderPath = ? OR folderPath LIKE ? || '/%' ORDER BY title COLLATE NOCASE ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, folder);
    _argIndex = 2;
    _statement.bindString(_argIndex, folder);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tracks"}, new Callable<List<Track>>() {
      @Override
      @NonNull
      public List<Track> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUri = CursorUtil.getColumnIndexOrThrow(_cursor, "uri");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfArtist = CursorUtil.getColumnIndexOrThrow(_cursor, "artist");
          final int _cursorIndexOfAlbum = CursorUtil.getColumnIndexOrThrow(_cursor, "album");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfTrackNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "trackNumber");
          final int _cursorIndexOfComposer = CursorUtil.getColumnIndexOrThrow(_cursor, "composer");
          final int _cursorIndexOfComment = CursorUtil.getColumnIndexOrThrow(_cursor, "comment");
          final int _cursorIndexOfDurationMs = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMs");
          final int _cursorIndexOfFolderPath = CursorUtil.getColumnIndexOrThrow(_cursor, "folderPath");
          final int _cursorIndexOfAlbumArtPath = CursorUtil.getColumnIndexOrThrow(_cursor, "albumArtPath");
          final int _cursorIndexOfHasMultipleArts = CursorUtil.getColumnIndexOrThrow(_cursor, "hasMultipleArts");
          final List<Track> _result = new ArrayList<Track>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Track _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUri;
            _tmpUri = _cursor.getString(_cursorIndexOfUri);
            final String _tmpFileName;
            _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpArtist;
            if (_cursor.isNull(_cursorIndexOfArtist)) {
              _tmpArtist = null;
            } else {
              _tmpArtist = _cursor.getString(_cursorIndexOfArtist);
            }
            final String _tmpAlbum;
            if (_cursor.isNull(_cursorIndexOfAlbum)) {
              _tmpAlbum = null;
            } else {
              _tmpAlbum = _cursor.getString(_cursorIndexOfAlbum);
            }
            final Integer _tmpYear;
            if (_cursor.isNull(_cursorIndexOfYear)) {
              _tmpYear = null;
            } else {
              _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            }
            final Integer _tmpTrackNumber;
            if (_cursor.isNull(_cursorIndexOfTrackNumber)) {
              _tmpTrackNumber = null;
            } else {
              _tmpTrackNumber = _cursor.getInt(_cursorIndexOfTrackNumber);
            }
            final String _tmpComposer;
            if (_cursor.isNull(_cursorIndexOfComposer)) {
              _tmpComposer = null;
            } else {
              _tmpComposer = _cursor.getString(_cursorIndexOfComposer);
            }
            final String _tmpComment;
            if (_cursor.isNull(_cursorIndexOfComment)) {
              _tmpComment = null;
            } else {
              _tmpComment = _cursor.getString(_cursorIndexOfComment);
            }
            final long _tmpDurationMs;
            _tmpDurationMs = _cursor.getLong(_cursorIndexOfDurationMs);
            final String _tmpFolderPath;
            _tmpFolderPath = _cursor.getString(_cursorIndexOfFolderPath);
            final String _tmpAlbumArtPath;
            if (_cursor.isNull(_cursorIndexOfAlbumArtPath)) {
              _tmpAlbumArtPath = null;
            } else {
              _tmpAlbumArtPath = _cursor.getString(_cursorIndexOfAlbumArtPath);
            }
            final boolean _tmpHasMultipleArts;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasMultipleArts);
            _tmpHasMultipleArts = _tmp != 0;
            _item = new Track(_tmpId,_tmpUri,_tmpFileName,_tmpTitle,_tmpArtist,_tmpAlbum,_tmpYear,_tmpTrackNumber,_tmpComposer,_tmpComment,_tmpDurationMs,_tmpFolderPath,_tmpAlbumArtPath,_tmpHasMultipleArts);
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
  public Object getTrackByUri(final String uri, final Continuation<? super Track> $completion) {
    final String _sql = "SELECT * FROM tracks WHERE uri = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, uri);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Track>() {
      @Override
      @Nullable
      public Track call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUri = CursorUtil.getColumnIndexOrThrow(_cursor, "uri");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfArtist = CursorUtil.getColumnIndexOrThrow(_cursor, "artist");
          final int _cursorIndexOfAlbum = CursorUtil.getColumnIndexOrThrow(_cursor, "album");
          final int _cursorIndexOfYear = CursorUtil.getColumnIndexOrThrow(_cursor, "year");
          final int _cursorIndexOfTrackNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "trackNumber");
          final int _cursorIndexOfComposer = CursorUtil.getColumnIndexOrThrow(_cursor, "composer");
          final int _cursorIndexOfComment = CursorUtil.getColumnIndexOrThrow(_cursor, "comment");
          final int _cursorIndexOfDurationMs = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMs");
          final int _cursorIndexOfFolderPath = CursorUtil.getColumnIndexOrThrow(_cursor, "folderPath");
          final int _cursorIndexOfAlbumArtPath = CursorUtil.getColumnIndexOrThrow(_cursor, "albumArtPath");
          final int _cursorIndexOfHasMultipleArts = CursorUtil.getColumnIndexOrThrow(_cursor, "hasMultipleArts");
          final Track _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUri;
            _tmpUri = _cursor.getString(_cursorIndexOfUri);
            final String _tmpFileName;
            _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpArtist;
            if (_cursor.isNull(_cursorIndexOfArtist)) {
              _tmpArtist = null;
            } else {
              _tmpArtist = _cursor.getString(_cursorIndexOfArtist);
            }
            final String _tmpAlbum;
            if (_cursor.isNull(_cursorIndexOfAlbum)) {
              _tmpAlbum = null;
            } else {
              _tmpAlbum = _cursor.getString(_cursorIndexOfAlbum);
            }
            final Integer _tmpYear;
            if (_cursor.isNull(_cursorIndexOfYear)) {
              _tmpYear = null;
            } else {
              _tmpYear = _cursor.getInt(_cursorIndexOfYear);
            }
            final Integer _tmpTrackNumber;
            if (_cursor.isNull(_cursorIndexOfTrackNumber)) {
              _tmpTrackNumber = null;
            } else {
              _tmpTrackNumber = _cursor.getInt(_cursorIndexOfTrackNumber);
            }
            final String _tmpComposer;
            if (_cursor.isNull(_cursorIndexOfComposer)) {
              _tmpComposer = null;
            } else {
              _tmpComposer = _cursor.getString(_cursorIndexOfComposer);
            }
            final String _tmpComment;
            if (_cursor.isNull(_cursorIndexOfComment)) {
              _tmpComment = null;
            } else {
              _tmpComment = _cursor.getString(_cursorIndexOfComment);
            }
            final long _tmpDurationMs;
            _tmpDurationMs = _cursor.getLong(_cursorIndexOfDurationMs);
            final String _tmpFolderPath;
            _tmpFolderPath = _cursor.getString(_cursorIndexOfFolderPath);
            final String _tmpAlbumArtPath;
            if (_cursor.isNull(_cursorIndexOfAlbumArtPath)) {
              _tmpAlbumArtPath = null;
            } else {
              _tmpAlbumArtPath = _cursor.getString(_cursorIndexOfAlbumArtPath);
            }
            final boolean _tmpHasMultipleArts;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasMultipleArts);
            _tmpHasMultipleArts = _tmp != 0;
            _result = new Track(_tmpId,_tmpUri,_tmpFileName,_tmpTitle,_tmpArtist,_tmpAlbum,_tmpYear,_tmpTrackNumber,_tmpComposer,_tmpComment,_tmpDurationMs,_tmpFolderPath,_tmpAlbumArtPath,_tmpHasMultipleArts);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getTrackCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM tracks";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
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
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getSourcesList(final Continuation<? super List<SourceFolder>> $completion) {
    final String _sql = "SELECT * FROM source_folders";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<SourceFolder>>() {
      @Override
      @NonNull
      public List<SourceFolder> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUri = CursorUtil.getColumnIndexOrThrow(_cursor, "uri");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfIsDefault = CursorUtil.getColumnIndexOrThrow(_cursor, "isDefault");
          final int _cursorIndexOfLastScanned = CursorUtil.getColumnIndexOrThrow(_cursor, "lastScanned");
          final List<SourceFolder> _result = new ArrayList<SourceFolder>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SourceFolder _item;
            final String _tmpUri;
            _tmpUri = _cursor.getString(_cursorIndexOfUri);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final boolean _tmpIsDefault;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDefault);
            _tmpIsDefault = _tmp != 0;
            final long _tmpLastScanned;
            _tmpLastScanned = _cursor.getLong(_cursorIndexOfLastScanned);
            _item = new SourceFolder(_tmpUri,_tmpDisplayName,_tmpIsDefault,_tmpLastScanned);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getArtsForTrack(final long trackId,
      final Continuation<? super List<AlbumArt>> $completion) {
    final String _sql = "SELECT * FROM album_arts WHERE trackId = ? ORDER BY isPrimary DESC, pictureType ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, trackId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<AlbumArt>>() {
      @Override
      @NonNull
      public List<AlbumArt> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTrackId = CursorUtil.getColumnIndexOrThrow(_cursor, "trackId");
          final int _cursorIndexOfArtPath = CursorUtil.getColumnIndexOrThrow(_cursor, "artPath");
          final int _cursorIndexOfPictureType = CursorUtil.getColumnIndexOrThrow(_cursor, "pictureType");
          final int _cursorIndexOfIsPrimary = CursorUtil.getColumnIndexOrThrow(_cursor, "isPrimary");
          final List<AlbumArt> _result = new ArrayList<AlbumArt>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AlbumArt _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTrackId;
            _tmpTrackId = _cursor.getLong(_cursorIndexOfTrackId);
            final String _tmpArtPath;
            _tmpArtPath = _cursor.getString(_cursorIndexOfArtPath);
            final int _tmpPictureType;
            _tmpPictureType = _cursor.getInt(_cursorIndexOfPictureType);
            final boolean _tmpIsPrimary;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPrimary);
            _tmpIsPrimary = _tmp != 0;
            _item = new AlbumArt(_tmpId,_tmpTrackId,_tmpArtPath,_tmpPictureType,_tmpIsPrimary);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SourceFolder>> getSources() {
    final String _sql = "SELECT * FROM source_folders";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"source_folders"}, new Callable<List<SourceFolder>>() {
      @Override
      @NonNull
      public List<SourceFolder> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUri = CursorUtil.getColumnIndexOrThrow(_cursor, "uri");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfIsDefault = CursorUtil.getColumnIndexOrThrow(_cursor, "isDefault");
          final int _cursorIndexOfLastScanned = CursorUtil.getColumnIndexOrThrow(_cursor, "lastScanned");
          final List<SourceFolder> _result = new ArrayList<SourceFolder>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SourceFolder _item;
            final String _tmpUri;
            _tmpUri = _cursor.getString(_cursorIndexOfUri);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final boolean _tmpIsDefault;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDefault);
            _tmpIsDefault = _tmp != 0;
            final long _tmpLastScanned;
            _tmpLastScanned = _cursor.getLong(_cursorIndexOfLastScanned);
            _item = new SourceFolder(_tmpUri,_tmpDisplayName,_tmpIsDefault,_tmpLastScanned);
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
  public Object getDefaultSource(final Continuation<? super SourceFolder> $completion) {
    final String _sql = "SELECT * FROM source_folders WHERE isDefault = 1 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SourceFolder>() {
      @Override
      @Nullable
      public SourceFolder call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfUri = CursorUtil.getColumnIndexOrThrow(_cursor, "uri");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfIsDefault = CursorUtil.getColumnIndexOrThrow(_cursor, "isDefault");
          final int _cursorIndexOfLastScanned = CursorUtil.getColumnIndexOrThrow(_cursor, "lastScanned");
          final SourceFolder _result;
          if (_cursor.moveToFirst()) {
            final String _tmpUri;
            _tmpUri = _cursor.getString(_cursorIndexOfUri);
            final String _tmpDisplayName;
            _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            final boolean _tmpIsDefault;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDefault);
            _tmpIsDefault = _tmp != 0;
            final long _tmpLastScanned;
            _tmpLastScanned = _cursor.getLong(_cursorIndexOfLastScanned);
            _result = new SourceFolder(_tmpUri,_tmpDisplayName,_tmpIsDefault,_tmpLastScanned);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
