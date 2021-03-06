package tahrir.tools;

import java.io.*;
import java.util.Map;
import java.util.zip.*;

import tahrir.io.serialization.*;

import com.google.common.collect.MapMaker;

public class Persistence {
	private static final Map<File, Tuple2<Long, Object>> cache = new MapMaker().makeMap();

	public static <T> void loadAndModify(final Class<T> cls, final File f, final ModifyBlock<T> mb) {
		synchronized (f) {
				final T object = loadReadOnly(cls, f);
				final Modified wasModified = new Modified();
				mb.run(object, wasModified);
				if (wasModified.isModified()) {
					save(f, object);
				}
		}
	}

	public static <T> void save(final File f, final T object) {
		try {
			final DataOutputStream dos = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(f, false)));
			TrSerializer.serializeTo(object, dos);
			dos.close();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		} catch (final TrSerializableException e) {
			throw new RuntimeException(e);
		}
		cache.put(f, Tuple2.<Long, Object> of(f.lastModified(), object));
	}

	@SuppressWarnings("unchecked")
	public static <T> T loadReadOnly(final Class<T> cls, final File f) {
		try {
			final Tuple2<Long, Object> cached = cache.get(f);
			if (cached != null && cached.a > f.lastModified())
				return (T) cached.b;
			else {
				final DataInputStream dis = new DataInputStream(new GZIPInputStream(new FileInputStream(f)));
				final T object = TrSerializer.deserializeFrom(cls, dis);
				dis.close();
				cache.put(f, Tuple2.<Long, Object> of(f.lastModified(), object));
				return object;
			}
		} catch (final TrSerializableException e) {
			throw new RuntimeException(e);
		} catch (final FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static interface ModifyBlock<T> {
		public void run(T object, Modified modified);
	}

	public static class Modified {
		private boolean modified = true;

		public void notModified() {
			modified = false;
		}

		public boolean isModified() {
			return modified;
		}
	}
}
