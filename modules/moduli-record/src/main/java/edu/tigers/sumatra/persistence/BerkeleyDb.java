/*
 * Copyright (c) 2009 - 2022, DHBW Mannheim - TIGERs Mannheim
 */

package edu.tigers.sumatra.persistence;

import edu.tigers.sumatra.model.SumatraModel;
import lombok.extern.log4j.Log4j2;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Consumer;


/**
 * Berkeley database
 */
@Log4j2
public class BerkeleyDb
{
	private final BerkeleyEnv env = new BerkeleyEnv();
	private final Path dbPath;
	private final Map<Class<?>, IBerkeleyAccessor<?>> accessors = new HashMap<>();


	/**
	 * @param dbPath absolute path to database folder or zip file
	 */
	public BerkeleyDb(final Path dbPath)
	{
		if (dbPath.toString().endsWith(".zip"))
		{
			String folderName = determineFolderName(dbPath.toFile());
			this.dbPath = Paths.get(dbPath.toString().substring(0, dbPath.toString().length() - 4));
			if (!this.dbPath.toFile().exists())
			{
				unpackDatabase(dbPath.toFile(), folderName);
			} else
			{
				log.info("Database is already extracted, using: {}", this.dbPath);
			}
		} else
		{
			this.dbPath = dbPath;
		}
	}


	private String determineFolderName(final File file)
	{
		try (ZipFile zipFile = new ZipFile(file))
		{
			String topLevel = null;
			for (FileHeader fh : zipFile.getFileHeaders())
			{
				String root = fh.getFileName().split("/")[0];
				if (root.equals(topLevel) || topLevel == null)
				{
					topLevel = root;
				} else
				{
					log.error("Recording contains more than one folder, this is invalid");
					return file.getName();
				}
			}
			return topLevel;
		} catch (IOException e)
		{
			log.error("Error while processing zip recording: ", e);
		}
		return file.getName();
	}


	/**
	 * @param matchType
	 * @param stage
	 * @param teamYellow name of yellow team
	 * @param teamBlue   name of blue team
	 * @return a new empty unopened database at the default location
	 */
	public static BerkeleyDb withDefaultLocation(String matchType, String stage, String teamYellow, String teamBlue)
	{
		return new BerkeleyDb(Paths.get(getDefaultBasePath(), getDefaultName(matchType, stage, teamYellow, teamBlue)));
	}


	/**
	 * @param customLocation a custom absolute path to a database
	 * @return a new database handle with a custom name at the default base path
	 */
	public static BerkeleyDb withCustomLocation(final Path customLocation)
	{
		return new BerkeleyDb(customLocation);
	}


	/**
	 * @param matchType  type of match
	 * @param stage      stage of game
	 * @param teamYellow name of yellow team
	 * @param teamBlue   name of blue team
	 * @return the default name for a new database
	 */
	public static String getDefaultName(String matchType, String stage, String teamYellow, String teamBlue)
	{
		SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		dt.setTimeZone(TimeZone.getDefault());
		return dt.format(new Date()) + String.format("-%s-%s-%s-vs-%s", matchType, stage, teamYellow, teamBlue);
	}


	/**
	 * @return
	 */
	public static String getDefaultBasePath()
	{
		return SumatraModel.getInstance()
				.getUserProperty("edu.tigers.sumatra.persistence.basePath", "data/record");
	}


	public <T> void add(Class<T> clazz, IBerkeleyAccessor<T> accessor)
	{
		accessors.put(clazz, accessor);
	}


	public Set<Class<?>> getAccessorTypes()
	{
		return Collections.unmodifiableSet(accessors.keySet());
	}


	public <T> T get(Class<T> clazz, long key)
	{
		IBerkeleyAccessor<T> accessor = getAccessor(clazz);
		if (accessor == null)
		{
			return null;
		}
		return accessor.get(key);
	}


	public <T> List<T> getAll(Class<T> clazz)
	{
		IBerkeleyAccessor<T> accessor = getAccessor(clazz);
		if (accessor == null)
		{
			return Collections.emptyList();
		}
		return accessor.load();
	}


	public <T> void forEach(Class<T> clazz, Consumer<T> consumer)
	{
		IBerkeleyAccessor<T> accessor = getAccessor(clazz);
		if (accessor == null)
		{
			return;
		}
		accessor.forEach(consumer);
	}


	public <T> long size(Class<T> clazz)
	{
		IBerkeleyAccessor<T> accessor = getAccessor(clazz);
		if (accessor == null)
		{
			return 0;
		}
		return accessor.size();
	}


	public <T> void write(Class<T> clazz, Collection<T> elements)
	{
		IBerkeleyAccessor<T> accessor = getAccessor(clazz);
		if (accessor != null)
		{
			accessor.write(elements);
		}
	}


	public <T> void write(Class<T> clazz, T element)
	{
		IBerkeleyAccessor<T> accessor = getAccessor(clazz);
		if (accessor != null)
		{
			accessor.write(element);
		}
	}


	@SuppressWarnings("unchecked")
	private <T> IBerkeleyAccessor<T> getAccessor(Class<T> clazz)
	{
		return (IBerkeleyAccessor<T>) accessors.get(clazz);
	}


	private void unpackDatabase(final File file, final String folderName)
	{
		log.info("Unpacking database: {}", file);
		try (ZipFile zipFile = new ZipFile(file))
		{
			zipFile.extractAll(dbPath.toFile().getParent());

			File extracted = Paths.get(dbPath.toFile().getParent(), folderName).toFile();

			Files.move(extracted.toPath(), Paths.get(dbPath.getParent().toString(), file.getName().replace(".zip", "")));

			log.info("Unpacking finished.");
		} catch (ZipException e)
		{
			log.error("Unpacking failed.", e);
		} catch (IOException e)
		{
			log.error("Could not move extracted replay: ", e);
		}
	}


	/**
	 * Open Database
	 */
	public void open()
	{
		env.open(dbPath.toFile());
		accessors.values().forEach(a -> a.open(env.getEntityStore()));
	}


	/**
	 * Close database
	 */
	public void close()
	{
		env.close();
	}


	/**
	 * Delete the database from filesystem
	 *
	 * @throws IOException
	 */
	public void delete() throws IOException
	{
		if (env.isOpen())
		{
			throw new IllegalStateException("Database must be closed before deletion.");
		}
		FileUtils.deleteDirectory(dbPath.toFile());
	}


	/**
	 * Compress the database
	 *
	 * @throws IOException
	 */
	public void compress() throws IOException
	{
		env.compress();
	}


	/**
	 * @return the env
	 */
	public final BerkeleyEnv getEnv()
	{
		return env;
	}


	public Long getFirstKey()
	{
		return accessors.values().stream()
				.filter(IBerkeleyAccessor::isSumatraTimestampBased)
				.map(IBerkeleyAccessor::getFirstKey)
				.filter(Objects::nonNull)
				.reduce(this::getSmallerKey)
				.orElse(null);
	}


	public Long getLastKey()
	{
		return accessors.values().stream()
				.filter(IBerkeleyAccessor::isSumatraTimestampBased)
				.map(IBerkeleyAccessor::getLastKey)
				.filter(Objects::nonNull)
				.reduce(this::getLargerKey)
				.orElse(null);
	}


	public Long getNextKey(long tCur)
	{
		return accessors.values().stream()
				.filter(IBerkeleyAccessor::isSumatraTimestampBased)
				.map(s -> s.getNextKey(tCur))
				.reduce((k1, k2) -> getNearestKey(tCur, k1, k2))
				.orElse(null);
	}


	public Long getPreviousKey(long tCur)
	{
		return accessors.values().stream()
				.filter(IBerkeleyAccessor::isSumatraTimestampBased)
				.map(s -> s.getPreviousKey(tCur))
				.reduce((k1, k2) -> getNearestKey(tCur, k1, k2))
				.orElse(null);
	}


	public Long getKey(long tCur)
	{
		return accessors.values().stream()
				.filter(IBerkeleyAccessor::isSumatraTimestampBased)
				.map(s -> s.getNearestKey(tCur))
				.reduce((k1, k2) -> getNearestKey(tCur, k1, k2))
				.orElse(null);
	}


	private Long getLargerKey(final Long k1, final Long k2)
	{
		if (k1 == null)
		{
			return k2;
		}
		if (k2 == null)
		{
			return k1;
		}
		if (k1 > k2)
		{
			return k1;
		}
		return k2;
	}


	private Long getSmallerKey(final Long k1, final Long k2)
	{
		if (k1 == null)
		{
			return k2;
		}
		if (k2 == null)
		{
			return k1;
		}
		if (k1 < k2)
		{
			return k1;
		}
		return k2;
	}


	private Long getNearestKey(final long key, final Long k1, final Long k2)
	{
		if (k1 == null)
		{
			return k2;
		}
		if (k2 == null)
		{
			return k1;
		}
		long diff1 = Math.abs(key - k1);
		long diff2 = Math.abs(key - k2);

		if (diff1 < diff2)
		{
			return k1;
		}
		return k2;
	}


	/**
	 * @return the db path
	 */
	public String getDbPath()
	{
		return dbPath.toString();
	}
}
