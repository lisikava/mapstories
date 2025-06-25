package wat;

import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

public class LostAndFoundGeminiTest {
    
    @Test
    void testWalletMatch()
    throws ExecutionException, InterruptedException, TimeoutException {
        // Test case: Should match - similar wallets
        String lostWallet = "Couldn't find " +
                "my wallet right after I left the inner space of Mogilskie " +
                "roundabout. Brown leather wallet, had around 45 euros plus change inside.";
        String foundWallet = "Found brown " +
                "leather wallet near Mogilskie roundabout with euros inside";
        CompletableFuture<Boolean>
                result = LostAndFoundMatcher.descriptionsMatch(lostWallet, foundWallet);
        System.out.println("Match result: " + result);
        assertTrue(result.get(30, TimeUnit.SECONDS), "Similar brown leather wallets " +
                "should match");
    }

    @Test
    void testWalletNoMatch()
    throws ExecutionException, InterruptedException, TimeoutException {
        // Test case: Shouldn't match - leather wallet and cowboy's monologue
        String lostWallet = "I am a cowboy. Sometimes I find, sometimes I do " +
                "not. What is it to finding, anyways? Maybe I am happy if I " +
                "find, maybe if I return. Regardless, I want to find myself";
        String foundWallet = "Found brown " +
                "leather wallet near Mogilskie roundabout with euros inside";
        CompletableFuture<Boolean>
                result = LostAndFoundMatcher.descriptionsMatch(lostWallet, foundWallet);
        System.out.println("Match result: " + result);
        assertFalse(result.get(30, TimeUnit.SECONDS), "Brown leather wallet " +
                "should not match cowboy's delirium");
    }
}