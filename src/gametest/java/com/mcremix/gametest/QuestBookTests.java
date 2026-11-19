package com.mcremix.gametest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.mcremix.McRemix;
import com.mcremix.compat.QuestBook;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The FTB Quests book the mod ships (made by tools/gen_quests.py) and the installer that copies it into
 * config/ftbquests/quests. FTB Quests itself is not loaded here, so these check what the book names against the game's
 * registries, and the installer against a folder of its own.
 */
@GameTestHolder(McRemix.ID)
@PrefixGameTestTemplate(false)
public class QuestBookTests {
	private static final Pattern ITEM = Pattern.compile("^\\s*(?:item|icon): \"([^\"]+)\"$", Pattern.MULTILINE);
	private static final Pattern OBSERVE_TYPE = Pattern.compile("^\\s*observe_type: (\\d+)$", Pattern.MULTILINE);
	private static final Pattern TO_OBSERVE = Pattern.compile("^\\s*to_observe: \"([^\"]+)\"$", Pattern.MULTILINE);
	private static final Pattern STRUCTURE = Pattern.compile("^\\s*structure: \"#([^\"]+)\"$", Pattern.MULTILINE);
	private static final Pattern ID = Pattern.compile("^\\s*id: \"([0-9A-F]{16})\"$", Pattern.MULTILINE);
	private static final Pattern DEPENDENCIES = Pattern.compile("dependencies: \\[([^\\]]*)\\]");
	private static final Pattern QUOTED = Pattern.compile("\"([0-9A-F]{16})\"");

	private static List<String> all(Pattern pattern, String text) {
		List<String> found = new ArrayList<>();
		Matcher m = pattern.matcher(text);
		while (m.find())
			found.add(m.group(1));
		return found;
	}

	/**
	 * Every item a quest asks for or shows exists, every block state a quest looks for parses against its block, every
	 * block tag it looks for has blocks in it, the structure tag the ruins quest wants exists, and every quest a quest
	 * waits on is in the book.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void questBookNamesThingsThatExist(GameTestHelper h) throws IOException {
		QuestBook.Book book = QuestBook.Book.read(QuestBook::resource);
		h.assertTrue(book.files().keySet().stream().filter(n -> n.startsWith("chapters/")).count() >= 10,
			"the book should have its chapters");
		Set<String> ids = new HashSet<>(), wanted = new HashSet<>();
		int observations = 0;
		for (Map.Entry<String, byte[]> file : book.files().entrySet()) {
			String name = file.getKey(), text = new String(file.getValue(), StandardCharsets.UTF_8);
			for (String item : all(ITEM, text)) {
				ResourceLocation id = ResourceLocation.tryParse(item);
				h.assertTrue(id != null && ForgeRegistries.ITEMS.containsKey(id)
					&& ForgeRegistries.ITEMS.getValue(id) != Items.AIR, name + " names an item that doesn't exist: " + item);
			}
			List<String> types = all(OBSERVE_TYPE, text), targets = all(TO_OBSERVE, text);
			h.assertTrue(types.size() == targets.size(), name + " should give each observation one thing to look at");
			for (int i = 0; i < types.size(); i++, observations++) {
				String target = targets.get(i);
				if (target.startsWith("#")) {
					h.assertTrue(types.get(i).equals("1"), name + " should look for " + target + " as a block tag");
					var tag = BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, new ResourceLocation(target.substring(1))));
					h.assertTrue(tag.isPresent() && tag.get().size() > 0, name + " looks for an empty block tag: " + target);
				} else {
					h.assertTrue(types.get(i).equals("2"), name + " should look for " + target + " as a block state");
					try {
						BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), target, false);
					} catch (CommandSyntaxException e) {
						throw new AssertionError(name + " looks for a block state that can't be: " + target + " (" + e.getMessage() + ")");
					}
				}
			}
			for (String structure : all(STRUCTURE, text)) {
				var tag = h.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE)
					.getTag(TagKey.create(Registries.STRUCTURE, new ResourceLocation(structure)));
				h.assertTrue(tag.isPresent() && tag.get().size() > 0, name + " wants a structure tag that's empty: #" + structure);
			}
			for (String id : all(ID, text))
				h.assertTrue(ids.add(id), name + " repeats the id " + id);
			for (String list : all(DEPENDENCIES, text))
				wanted.addAll(all(QUOTED, list));
		}
		h.assertTrue(observations > 0, "the book should have quests that look at a working block");
		wanted.removeAll(ids);
		h.assertTrue(wanted.isEmpty(), "quests wait on quests that aren't in the book: " + wanted);
		h.succeed();
	}

	/**
	 * The installer writes the book into an empty folder (or one holding only FTB Quests' empty book), leaves it be at
	 * the same version, replaces an earlier version only while nobody has changed it (deleting a chapter the new book no
	 * longer has), and never writes over a changed book, a book of someone else's, or anything outside the folder.
	 */
	@GameTest(template = GT.PLATFORM)
	public static void questBookInstallsOnlyOverItsOwnUnchangedBook(GameTestHelper h) throws IOException {
		Path tmp = Files.createTempDirectory("mcremix_questbook");
		try {
			QuestBook.Book book = QuestBook.Book.read(QuestBook::resource);
			Path a = tmp.resolve("a"), marker = a.resolve(QuestBook.MARKER);
			h.assertTrue(QuestBook.install(a, QuestBook::resource) == QuestBook.Result.INSTALLED, "an empty folder should get the book");
			for (Map.Entry<String, byte[]> file : book.files().entrySet())
				h.assertTrue(Arrays.equals(Files.readAllBytes(a.resolve(file.getKey())), file.getValue()), file.getKey() + " should be written");
			h.assertTrue(Files.isRegularFile(marker), "the marker should be written");
			h.assertTrue(QuestBook.install(a, QuestBook::resource) == QuestBook.Result.UP_TO_DATE, "the same version should be left be");

			// an earlier version, untouched, with a chapter this one doesn't have
			byte[] old = "{ }\n".getBytes(StandardCharsets.UTF_8);
			Files.write(a.resolve("chapters/gone.snbt"), old);
			Files.writeString(marker, Files.readString(marker).replace("book " + book.version(), "book 0")
				+ sha1(old) + " chapters/gone.snbt\n");
			h.assertTrue(QuestBook.install(a, QuestBook::resource) == QuestBook.Result.UPDATED, "an unchanged earlier version should be updated");
			h.assertTrue(!Files.exists(a.resolve("chapters/gone.snbt")), "a chapter the new book doesn't have should go");
			h.assertTrue(Files.readString(marker).contains("book " + book.version()), "the marker should name the new version");

			// an earlier version someone changed in the editor
			String someChapter = book.files().keySet().stream().filter(n -> n.startsWith("chapters/")).findFirst().orElseThrow();
			Files.writeString(a.resolve(someChapter), "\n", StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);
			Files.writeString(marker, Files.readString(marker).replace("book " + book.version(), "book 0"));
			h.assertTrue(QuestBook.install(a, QuestBook::resource) == QuestBook.Result.KEPT_CHANGED, "a changed book should be kept");
			h.assertTrue(!Arrays.equals(Files.readAllBytes(a.resolve(someChapter)), book.files().get(someChapter)), "the change should stay");

			// an earlier version with a chapter someone added
			Path c = tmp.resolve("c");
			QuestBook.install(c, QuestBook::resource);
			Files.writeString(c.resolve(QuestBook.MARKER), Files.readString(c.resolve(QuestBook.MARKER)).replace("book " + book.version(), "book 0"));
			Files.writeString(c.resolve("chapters/added.snbt"), "{ }\n");
			h.assertTrue(QuestBook.install(c, QuestBook::resource) == QuestBook.Result.KEPT_CHANGED, "a book with an added chapter should be kept");

			// someone else's book, and FTB Quests' own empty book
			Path b = tmp.resolve("b");
			Files.createDirectories(b.resolve("chapters"));
			Files.writeString(b.resolve("chapters/mine.snbt"), "{ }\n");
			h.assertTrue(QuestBook.install(b, QuestBook::resource) == QuestBook.Result.KEPT_OTHER, "another book should be kept");
			h.assertTrue(!Files.exists(b.resolve(QuestBook.MARKER)), "another book should get no marker");
			Path e = tmp.resolve("e");
			Files.createDirectories(e);
			Files.writeString(e.resolve("data.snbt"), "{ }\n");
			h.assertTrue(QuestBook.install(e, QuestBook::resource) == QuestBook.Result.INSTALLED, "FTB Quests' empty book should be filled");

			// a marker naming a file outside the folder
			Path d = tmp.resolve("d"), outside = tmp.resolve("outside.txt");
			Files.createDirectories(d);
			Files.write(outside, old);
			Files.writeString(d.resolve(QuestBook.MARKER), "book 0\n" + sha1(old) + " ../outside.txt\n");
			boolean refused = false;
			try {
				QuestBook.install(d, QuestBook::resource);
			} catch (IOException ex) {
				refused = true;
			}
			h.assertTrue(refused && Files.exists(outside), "a marker naming a file outside the folder should be refused");
		} finally {
			try (Stream<Path> walk = Files.walk(tmp)) {
				walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
			}
		}
		h.succeed();
	}

	private static String sha1(byte[] bytes) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
