package dev.infinitedimensions.dimension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;

public final class DimensionIds {
    /**
     * The original snapshot hashes the text with SHA-256 plus a salt that isn't publicly documented,
     * so numeric dimension ids will not match Mojang's. Same text -> same dimension is preserved.
     */
    private static final String SALT = "infinitedimensions:v1:";

    private DimensionIds() {}

    /** A plain positive number is used as the id directly ("/warp 3"), anything else is hashed. */
    public static int fromText(String text) {
        String t = text.strip();
        if (t.matches("\\d{1,10}")) {
            long v = Long.parseLong(t);
            if (v <= Integer.MAX_VALUE) return (int) v;
        }
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] h = sha.digest((SALT + t).getBytes(StandardCharsets.UTF_8));
            int v = ((h[0] & 0xFF) << 24) | ((h[1] & 0xFF) << 16) | ((h[2] & 0xFF) << 8) | (h[3] & 0xFF);
            return v & 0x7FFFFFFF;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean isBook(ItemStack stack) {
        return stack.has(DataComponents.WRITTEN_BOOK_CONTENT) || stack.has(DataComponents.WRITABLE_BOOK_CONTENT);
    }

    /** Pages joined with newlines, so a one-page book equals "/warp <that text>". Empty if not a book. */
    public static String bookText(ItemStack stack) {
        WrittenBookContent written = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (written != null) {
            return written.pages().stream()
                    .map(page -> page.raw().getString())
                    .collect(Collectors.joining("\n"))
                    .strip();
        }
        WritableBookContent writable = stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
        if (writable != null) {
            return writable.pages().stream()
                    .map(page -> page.raw())
                    .collect(Collectors.joining("\n"))
                    .strip();
        }
        return "";
    }
}
