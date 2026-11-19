package com.mcremix.compat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.mcremix.McRemix;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Installs the mod's FTB Quests book into config/ftbquests/quests when FTB Quests is loaded and has no book there yet,
 * and keeps it up to date with the mod for as long as nobody changes it. FTB Quests stays optional: without it this
 * does nothing.
 * <p>
 * The book is made by tools/gen_quests.py into resources/mcremix_questbook, whose index.txt names the book's version
 * (a hash of its files) and every file in it. Installing writes {@value #MARKER} into the book folder with that
 * version and a hash of each file written. When the mod brings a new version, the book is replaced only if every file
 * still matches its hash and no chapter was added: a book changed in FTB Quests' editor is left alone, as is a book
 * that came with a modpack (no marker) or one whose marker was deleted. Quest ids come from fixed keys in the
 * generator, so progress carries over when the book is replaced.
 */
public final class QuestBook {
	static final String RESOURCES = "/mcremix_questbook/";
	static final String INDEX = "index.txt";
	public static final String MARKER = "mcremix_book.txt";

	public enum Result {
		/** There was no book: this one was written. */
		INSTALLED,
		/** An earlier, unchanged version of this book was replaced. */
		UPDATED,
		/** This version is already there. */
		UP_TO_DATE,
		/** An earlier version of this book is there but was changed since, so it was left alone. */
		KEPT_CHANGED,
		/** Another book is there (no marker), so it was left alone. */
		KEPT_OTHER
	}

	private QuestBook() {}

	/** Called once at common setup. */
	public static void install() {
		if (!ModList.get().isLoaded("ftbquests"))
			return;
		Path folder = FMLPaths.CONFIGDIR.get().resolve("ftbquests").resolve("quests");
		try {
			switch (install(folder, QuestBook::resource)) {
				case INSTALLED -> McRemix.LOGGER.info("Installed the Minecraft Remix quest book in {}", folder);
				case UPDATED -> McRemix.LOGGER.info("Updated the Minecraft Remix quest book in {}", folder);
				case KEPT_CHANGED -> McRemix.LOGGER.info("The quest book in {} was changed after Minecraft Remix "
					+ "installed it, so this version's book was not written over it. Delete the folder to get the new "
					+ "book (quest progress is kept)", folder);
				case KEPT_OTHER -> McRemix.LOGGER.info("Kept the quest book already in {}; delete the folder to get "
					+ "Minecraft Remix's book", folder);
				case UP_TO_DATE -> {}
			}
		} catch (IOException | RuntimeException e) {
			McRemix.LOGGER.error("Could not install the Minecraft Remix quest book in {}", folder, e);
		}
	}

	/** Reads one of the book's files from the mod jar, by its path in the book folder. */
	public interface Source {
		byte[] read(String name) throws IOException;
	}

	public static byte[] resource(String name) throws IOException {
		try (InputStream in = QuestBook.class.getResourceAsStream(RESOURCES + name)) {
			if (in == null)
				throw new IOException("missing from the mod jar: " + RESOURCES + name);
			return in.readAllBytes();
		}
	}

	/** Installs or updates the book in {@code folder} from {@code source}, as the class comment says. */
	public static Result install(Path folder, Source source) throws IOException {
		Book book = Book.read(source);
		Path marker = folder.resolve(MARKER);
		if (Files.isRegularFile(marker)) {
			Marker old = Marker.read(marker);
			if (book.version.equals(old.version))
				return Result.UP_TO_DATE;
			if (!old.untouched(folder))
				return Result.KEPT_CHANGED;
			for (String name : old.files.keySet())
				Files.deleteIfExists(inside(folder, name));
			book.write(folder);
			return Result.UPDATED;
		}
		try (Stream<Path> chapters = chapters(folder)) {
			if (chapters.findAny().isPresent())
				return Result.KEPT_OTHER;
		}
		book.write(folder);
		return Result.INSTALLED;
	}

	/** The book's files, by their paths in the book folder, and its version. */
	public record Book(String version, Map<String, byte[]> files) {
		public static Book read(Source source) throws IOException {
			String version = null;
			Map<String, byte[]> files = new LinkedHashMap<>();
			for (String line : new String(source.read(INDEX), StandardCharsets.UTF_8).split("\n")) {
				line = line.strip();
				if (line.isEmpty() || line.startsWith("#"))
					continue;
				if (line.startsWith("book "))
					version = line.substring(5).strip();
				else
					files.put(line, source.read(line));
			}
			if (version == null || files.isEmpty())
				throw new IOException("the quest book's index names no version or no files");
			return new Book(version, files);
		}

		void write(Path folder) throws IOException {
			StringBuilder marker = new StringBuilder()
				.append("# Minecraft Remix wrote this quest book and keeps it up to date with the mod, as long as\n")
				.append("# none of the files below has changed. Delete this file to keep the book as it is for good.\n")
				.append("book ").append(version).append('\n');
			for (Map.Entry<String, byte[]> file : files.entrySet()) {
				Path path = inside(folder, file.getKey());
				Files.createDirectories(path.getParent());
				Files.write(path, file.getValue());
				marker.append(sha1(file.getValue())).append(' ').append(file.getKey()).append('\n');
			}
			Files.writeString(folder.resolve(MARKER), marker.toString(), StandardCharsets.UTF_8);
		}
	}

	/** What the marker says was installed: the version and each file's hash. */
	record Marker(String version, Map<String, String> files) {
		static Marker read(Path path) throws IOException {
			String version = "";
			Map<String, String> files = new LinkedHashMap<>();
			for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
				line = line.strip();
				if (line.isEmpty() || line.startsWith("#"))
					continue;
				int space = line.indexOf(' ');
				if (space < 0)
					continue;
				if (line.startsWith("book "))
					version = line.substring(5).strip();
				else
					files.put(line.substring(space + 1).strip(), line.substring(0, space));
			}
			return new Marker(version, files);
		}

		/** Whether every file written is still there unchanged, and no chapter was added. */
		boolean untouched(Path folder) throws IOException {
			for (Map.Entry<String, String> file : files.entrySet()) {
				Path path = inside(folder, file.getKey());
				if (!Files.isRegularFile(path) || !file.getValue().equals(sha1(Files.readAllBytes(path))))
					return false;
			}
			List<String> chapters = new ArrayList<>();
			try (Stream<Path> found = chapters(folder)) {
				found.forEach(p -> chapters.add(folder.relativize(p).toString().replace('\\', '/')));
			}
			return files.keySet().containsAll(chapters);
		}
	}

	private static Stream<Path> chapters(Path folder) throws IOException {
		Path dir = folder.resolve("chapters");
		if (!Files.isDirectory(dir))
			return Stream.empty();
		return Files.list(dir).filter(p -> p.getFileName().toString().endsWith(".snbt") && Files.isRegularFile(p));
	}

	/** {@code name} resolved in {@code folder}, refusing any name that would lead out of it. */
	private static Path inside(Path folder, String name) throws IOException {
		Path base = folder.toAbsolutePath().normalize(), path = base.resolve(name).normalize();
		if (!path.startsWith(base) || path.equals(base))
			throw new IOException("not a path inside the quest book folder: " + name);
		return path;
	}

	static String sha1(byte[] bytes) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
