package wat;

import org.junit.jupiter.api.Test;
import org.postgresql.geometric.PGpoint;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LostAndFoundGeminiTest {
    
    @Test
    void testPhoneMatch() {
        // Test case 1: Should match - iPhone vs smartphone
        Pin lostPhone = createPin("lost", "description", "Lost my black iPhone with red case at main square", "color", "black");
        Pin foundPhone = createPin("found", "description", "Found black smartphone with red case near fountain", "type", "smartphone");
        
        System.out.println("=== GEMINI PHONE MATCH TEST ===");
        System.out.println("Lost: " + LostAndFoundMatcher.preprocessPin(lostPhone));
        System.out.println("Found: " + LostAndFoundMatcher.preprocessPin(foundPhone));
        
        boolean result = LostAndFoundMatcher.matchSync(lostPhone, foundPhone);
        System.out.println("Match result: " + result);
        System.out.println("===============================");
        
        assertTrue(result, "iPhone and smartphone should match");
    }
    
    @Test
    void testWalletKeysNoMatch() {
        // Test case 2: Should not match - different items
        Pin lostWallet = createPin("lost", "description", "Lost brown leather wallet with euros", "color", "brown");
        Pin foundKeys = createPin("found", "description", "Found car keys with Toyota keychain", "type", "keys");
        
        System.out.println("=== GEMINI WALLET/KEYS NO MATCH TEST ===");
        System.out.println("Lost: " + LostAndFoundMatcher.preprocessPin(lostWallet));
        System.out.println("Found: " + LostAndFoundMatcher.preprocessPin(foundKeys));
        boolean result = LostAndFoundMatcher.matchSync(lostWallet, foundKeys);
        System.out.println("Match result: " + result);
        System.out.println("========================================");
        
        assertFalse(result, "Wallet and keys should not match");
    }
    
    @Test
    void testGlovesColorDifference() {
        // Test case 3: User's examples - different colored gloves
        Pin orangeGloves = createPin("lost", "description", "I've lost a pair of orange polyester gloves somewhere near Grzegorzeckie roundabout.", "email", "magdalena@mapstories.io");
        Pin whiteGloves = createPin("found", "description", "Found a pair of white knitted wool gloves on the pavement.", "email", "krakow.explorer@mapstories.io");
        
        System.out.println("=== GEMINI GLOVES COLOR TEST ===");
        System.out.println("Lost: " + LostAndFoundMatcher.preprocessPin(orangeGloves));
        System.out.println("Found: " + LostAndFoundMatcher.preprocessPin(whiteGloves));
        boolean result = LostAndFoundMatcher.matchSync(orangeGloves, whiteGloves);
        System.out.println("Match result: " + result);
        System.out.println("================================");
        
        assertFalse(result, "Orange and white gloves should not match due to color difference");
    }
    
    @Test
    void testWalletMatch() {
        // Test case 4: Should match - similar wallets
        Pin lostWallet = createPin("lost", "description", "Couldn't find my wallet right after I left the inner space of Mogilskie roundabout. Brown leather wallet, had around 45 euros plus change inside.", "email", "leonard@mapstories.io", "money", "45 eur");
        Pin foundWallet = createPin("found", "description", "Found brown leather wallet near Mogilskie roundabout with euros inside", "color", "brown", "material", "leather");
        
        System.out.println("=== GEMINI WALLET MATCH TEST ===");
        System.out.println("Lost: " + LostAndFoundMatcher.preprocessPin(lostWallet));
        System.out.println("Found: " + LostAndFoundMatcher.preprocessPin(foundWallet));
        boolean result = LostAndFoundMatcher.matchSync(lostWallet, foundWallet);
        System.out.println("Match result: " + result);
        System.out.println("=================================");
        
        assertTrue(result, "Similar brown leather wallets should match");
    }
    
    @Test
    void testBrandSynonyms() {
        // Test case 5: Brand synonyms - should match
        Pin lostBackpack = createPin("lost", "description", "Lost my Nike sports bag", "brand", "Nike", "color", "blue");
        Pin foundBackpack = createPin("found", "description", "Found blue Nike backpack in gym", "type", "backpack", "color", "blue");
        
        System.out.println("=== GEMINI BRAND SYNONYMS TEST ===");
        System.out.println("Lost: " + LostAndFoundMatcher.preprocessPin(lostBackpack));
        System.out.println("Found: " + LostAndFoundMatcher.preprocessPin(foundBackpack));
        boolean result = LostAndFoundMatcher.matchSync(lostBackpack, foundBackpack);
        System.out.println("Match result: " + result);
        System.out.println("===================================");
        
        assertTrue(result, "Nike sports bag and Nike backpack should match");
    }
    
    @Test
    void testSizeMatters() {
        // Test case 6: Different sizes - should not match
        Pin lostJacket = createPin("lost", "description", "Lost large black jacket", "size", "L", "color", "black");
        Pin foundJacket = createPin("found", "description", "Found small black jacket", "size", "S", "color", "black");
        
        System.out.println("=== GEMINI SIZE DIFFERENCE TEST ===");
        System.out.println("Lost: " + LostAndFoundMatcher.preprocessPin(lostJacket));
        System.out.println("Found: " + LostAndFoundMatcher.preprocessPin(foundJacket));
        boolean result = LostAndFoundMatcher.matchSync(lostJacket, foundJacket);
        System.out.println("Match result: " + result);
        System.out.println("====================================");
        
        assertFalse(result, "Large and small jackets should not match due to size difference");
    }
    
    @Test
    void testJewelryPrecision() {
        // Test case 7: Jewelry - should be very precise
        Pin lostRing = createPin("lost", "description", "Lost gold wedding ring with diamond", "material", "gold", "type", "ring");
        Pin foundRing = createPin("found", "description", "Found silver engagement ring with stone", "material", "silver", "type", "ring");
        
        System.out.println("=== GEMINI JEWELRY PRECISION TEST ===");
        System.out.println("Lost: " + LostAndFoundMatcher.preprocessPin(lostRing));
        System.out.println("Found: " + LostAndFoundMatcher.preprocessPin(foundRing));
        boolean result = LostAndFoundMatcher.matchSync(lostRing, foundRing);
        System.out.println("Match result: " + result);
        System.out.println("======================================");
        
        assertFalse(result, "Gold wedding ring and silver engagement ring should not match");
    }
    
    @Test
    void testElectronicsMatch() {
        // Test case 8: Electronics with accessories - should match
        Pin lostLaptop = createPin("lost", "description", "Lost MacBook Pro with charger in black case", "brand", "Apple", "type", "laptop");
        Pin foundLaptop = createPin("found", "description", "Found Apple laptop with power adapter in dark case", "brand", "Apple", "type", "computer");
        
        System.out.println("=== GEMINI ELECTRONICS MATCH TEST ===");
        System.out.println("Lost: " + LostAndFoundMatcher.preprocessPin(lostLaptop));
        System.out.println("Found: " + LostAndFoundMatcher.preprocessPin(foundLaptop));
        boolean result = LostAndFoundMatcher.matchSync(lostLaptop, foundLaptop);
        System.out.println("Match result: " + result);
        System.out.println("======================================");
        
        assertTrue(result, "MacBook Pro and Apple laptop should match");
    }
    
    @Test
    void testPetDescriptions() {
        // Test case 9: Pet descriptions - should match despite wording differences
        Pin lostDog = createPin("lost", "description", "Lost small brown terrier dog, very friendly, wearing red collar", "type", "dog", "color", "brown");
        Pin foundDog = createPin("found", "description", "Found friendly brown small dog with red collar, looks like terrier breed", "type", "dog", "color", "brown");
        
        System.out.println("=== GEMINI PET DESCRIPTIONS TEST ===");
        System.out.println("Lost: " + LostAndFoundMatcher.preprocessPin(lostDog));
        System.out.println("Found: " + LostAndFoundMatcher.preprocessPin(foundDog));
        boolean result = LostAndFoundMatcher.matchSync(lostDog, foundDog);
        System.out.println("Match result: " + result);
        System.out.println("=====================================");
        
        assertTrue(result, "Similar dog descriptions should match");
    }
    
    @Test
    void testCarKeysSpecificity() {
        // Test case 10: Car keys - should be specific about car details
        Pin lostKeys = createPin("lost", "description", "Lost Toyota Camry keys with blue keychain", "car", "Toyota Camry", "color", "blue");
        Pin foundKeys = createPin("found", "description", "Found Honda Civic keys with blue keychain", "car", "Honda Civic", "color", "blue");
        
        System.out.println("=== GEMINI CAR KEYS SPECIFICITY TEST ===");
        System.out.println("Lost: " + LostAndFoundMatcher.preprocessPin(lostKeys));
        System.out.println("Found: " + LostAndFoundMatcher.preprocessPin(foundKeys));
        boolean result = LostAndFoundMatcher.matchSync(lostKeys, foundKeys);
        System.out.println("Match result: " + result);
        System.out.println("========================================");
        
        assertFalse(result, "Toyota and Honda keys should not match despite same keychain color");
    }
    
    @Test
    void testClothingMaterials() {
        // Test case 11: Clothing materials - should consider material differences
        Pin lostScarf = createPin("lost", "description", "Lost wool scarf, gray color, very soft", "material", "wool", "color", "gray");
        Pin foundScarf = createPin("found", "description", "Found grey cotton scarf, soft texture", "material", "cotton", "color", "grey");
        
        System.out.println("=== GEMINI CLOTHING MATERIALS TEST ===");
        System.out.println("Lost: " + LostAndFoundMatcher.preprocessPin(lostScarf));
        System.out.println("Found: " + LostAndFoundMatcher.preprocessPin(foundScarf));
        boolean result = LostAndFoundMatcher.matchSync(lostScarf, foundScarf);
        System.out.println("Match result: " + result);
        System.out.println("=======================================");
        
        assertFalse(result, "Wool and cotton scarves should not match despite similar color");
    }
    
    @Test
    void testBookMatch() {
        // Test case 12: Books - should match despite minor description differences
        Pin lostBook = createPin("lost", "description", "Lost Harry Potter book, first one in series", "type", "book", "series", "Harry Potter");
        Pin foundBook = createPin("found", "description", "Found Harry Potter and the Philosopher's Stone book", "type", "book", "title", "Harry Potter");
        
        System.out.println("=== GEMINI BOOK MATCH TEST ===");
        System.out.println("Lost: " + LostAndFoundMatcher.preprocessPin(lostBook));
        System.out.println("Found: " + LostAndFoundMatcher.preprocessPin(foundBook));
        boolean result = LostAndFoundMatcher.matchSync(lostBook, foundBook);
        System.out.println("Match result: " + result);
        System.out.println("===============================");
        
        assertTrue(result, "Harry Potter first book descriptions should match");
    }
    
    @Test
    void testWatchPrecision() {
        // Test case 13: Watches - should be very specific about brand/model
        Pin lostWatch = createPin("lost", "description", "Lost Rolex Submariner watch, black face, steel bracelet", "brand", "Rolex", "model", "Submariner");
        Pin foundWatch = createPin("found", "description", "Found Omega Seamaster watch with black dial and metal band", "brand", "Omega", "model", "Seamaster");
        
        System.out.println("=== GEMINI WATCH PRECISION TEST ===");
        System.out.println("Lost: " + LostAndFoundMatcher.preprocessPin(lostWatch));
        System.out.println("Found: " + LostAndFoundMatcher.preprocessPin(foundWatch));
        boolean result = LostAndFoundMatcher.matchSync(lostWatch, foundWatch);
        System.out.println("Match result: " + result);
        System.out.println("====================================");
        
        assertFalse(result, "Rolex and Omega watches should not match despite similar appearance");
    }
    
    @Test
    void testUmbrellaColorMatch() {
        // Test case 14: Umbrellas - should match with color variations
        Pin lostUmbrella = createPin("lost", "description", "Lost black umbrella, automatic open/close", "color", "black", "type", "umbrella");
        Pin foundUmbrella = createPin("found", "description", "Found dark umbrella with push-button mechanism", "color", "dark", "type", "umbrella");
        
        System.out.println("=== GEMINI UMBRELLA COLOR TEST ===");
        System.out.println("Lost: " + LostAndFoundMatcher.preprocessPin(lostUmbrella));
        System.out.println("Found: " + LostAndFoundMatcher.preprocessPin(foundUmbrella));
        boolean result = LostAndFoundMatcher.matchSync(lostUmbrella, foundUmbrella);
        System.out.println("Match result: " + result);
        System.out.println("===================================");
        
        assertTrue(result, "Black umbrella and dark umbrella should match");
    }
    
    @Test
    void testGlassesPrescription() {
        // Test case 15: Glasses - should not match different prescriptions
        Pin lostGlasses = createPin("lost", "description", "Lost reading glasses, thick lenses, black frames", "type", "glasses", "prescription", "reading");
        Pin foundGlasses = createPin("found", "description", "Found sunglasses with dark lenses and black frames", "type", "sunglasses", "feature", "dark lenses");
        
        System.out.println("=== GEMINI GLASSES PRESCRIPTION TEST ===");
        System.out.println("Lost: " + LostAndFoundMatcher.preprocessPin(lostGlasses));
        System.out.println("Found: " + LostAndFoundMatcher.preprocessPin(foundGlasses));
        boolean result = LostAndFoundMatcher.matchSync(lostGlasses, foundGlasses);
        System.out.println("Match result: " + result);
        System.out.println("=========================================");
        
        assertFalse(result, "Reading glasses and sunglasses should not match");
    }
    
    @Test
    void testBagSizeFlexibility() {
        // Test case 16: Bags - should be flexible with size descriptions
        Pin lostBag = createPin("lost", "description", "Lost medium-sized handbag, brown leather with gold zipper", "size", "medium", "material", "leather");
        Pin foundBag = createPin("found", "description", "Found brown leather purse with golden zipper, regular size", "material", "leather", "color", "brown");
        
        System.out.println("=== GEMINI BAG SIZE FLEXIBILITY TEST ===");
        System.out.println("Lost: " + LostAndFoundMatcher.preprocessPin(lostBag));
        System.out.println("Found: " + LostAndFoundMatcher.preprocessPin(foundBag));
        boolean result = LostAndFoundMatcher.matchSync(lostBag, foundBag);
        System.out.println("Match result: " + result);
        System.out.println("========================================");
        
        assertTrue(result, "Medium handbag and regular purse should match");
    }
    
    @Test
    void testHeadphonesWireless() {
        // Test case 17: Headphones - wired vs wireless should not match
        Pin lostHeadphones = createPin("lost", "description", "Lost wireless AirPods in white charging case", "brand", "Apple", "type", "wireless");
        Pin foundHeadphones = createPin("found", "description", "Found white wired earphones with Apple connector", "brand", "Apple", "type", "wired");
        
        System.out.println("=== GEMINI HEADPHONES WIRELESS TEST ===");
        System.out.println("Lost: " + LostAndFoundMatcher.preprocessPin(lostHeadphones));
        System.out.println("Found: " + LostAndFoundMatcher.preprocessPin(foundHeadphones));
        boolean result = LostAndFoundMatcher.matchSync(lostHeadphones, foundHeadphones);
        System.out.println("Match result: " + result);
        System.out.println("========================================");
        
        assertFalse(result, "Wireless AirPods and wired earphones should not match");
    }
    
    @Test
    void testCatDescriptionMatch() {
        // Test case 18: Pet cats - should match with personality descriptions
        Pin lostCat = createPin("lost", "description", "Lost fluffy orange tabby cat, very shy, green eyes", "type", "cat", "color", "orange", "personality", "shy");
        Pin foundCat = createPin("found", "description", "Found orange striped cat with green eyes, seems timid and fluffy", "type", "cat", "color", "orange", "eyes", "green");
        
        System.out.println("=== GEMINI CAT DESCRIPTION TEST ===");
        System.out.println("Lost: " + LostAndFoundMatcher.preprocessPin(lostCat));
        System.out.println("Found: " + LostAndFoundMatcher.preprocessPin(foundCat));
        boolean result = LostAndFoundMatcher.matchSync(lostCat, foundCat);
        System.out.println("Match result: " + result);
        System.out.println("===================================");
        
        assertTrue(result, "Orange tabby cats with similar descriptions should match");
    }
    
    @Test
    void testBicycleSpecificity() {
        // Test case 19: Bicycles - should be specific about type and features
        Pin lostBike = createPin("lost", "description", "Lost red mountain bike, 21 gears, Trek brand", "type", "mountain bike", "brand", "Trek", "gears", "21");
        Pin foundBike = createPin("found", "description", "Found red road bike with multiple gears, Trek brand", "type", "road bike", "brand", "Trek", "color", "red");
        
        System.out.println("=== GEMINI BICYCLE SPECIFICITY TEST ===");
        System.out.println("Lost: " + LostAndFoundMatcher.preprocessPin(lostBike));
        System.out.println("Found: " + LostAndFoundMatcher.preprocessPin(foundBike));
        boolean result = LostAndFoundMatcher.matchSync(lostBike, foundBike);
        System.out.println("Match result: " + result);
        System.out.println("=======================================");
        
        assertFalse(result, "Mountain bike and road bike should not match despite same brand");
    }
    
    @Test
    void testToolsSimilarity() {
        // Test case 20: Tools - should match similar tools
        Pin lostTool = createPin("lost", "description", "Lost Phillips head screwdriver, red handle", "type", "screwdriver", "handle", "red", "head", "Phillips");
        Pin foundTool = createPin("found", "description", "Found red-handled Phillips screwdriver", "type", "screwdriver", "color", "red", "head_type", "Phillips");
        
        System.out.println("=== GEMINI TOOLS SIMILARITY TEST ===");
        System.out.println("Lost: " + LostAndFoundMatcher.preprocessPin(lostTool));
        System.out.println("Found: " + LostAndFoundMatcher.preprocessPin(foundTool));
        boolean result = LostAndFoundMatcher.matchSync(lostTool, foundTool);
        System.out.println("Match result: " + result);
        System.out.println("=====================================");
        
        assertTrue(result, "Phillips screwdrivers with red handles should match");
    }
    
    @Test
    void testMedicationSafety() {
        // Test case 21: Medications - should be very careful with medical items
        Pin lostMeds = createPin("lost", "description", "Lost bottle of ibuprofen 200mg tablets", "type", "medication", "drug", "ibuprofen", "dose", "200mg");
        Pin foundMeds = createPin("found", "description", "Found acetaminophen 500mg pill bottle", "type", "medication", "drug", "acetaminophen", "dose", "500mg");
        
        System.out.println("=== GEMINI MEDICATION SAFETY TEST ===");
        System.out.println("Lost: " + LostAndFoundMatcher.preprocessPin(lostMeds));
        System.out.println("Found: " + LostAndFoundMatcher.preprocessPin(foundMeds));
        boolean result = LostAndFoundMatcher.matchSync(lostMeds, foundMeds);
        System.out.println("Match result: " + result);
        System.out.println("======================================");
        
        assertFalse(result, "Different medications should never match for safety");
    }
    
    @Test
    void testChildToyMatch() {
        // Test case 22: Children's toys - should match with brand flexibility
        Pin lostToy = createPin("lost", "description", "Lost small teddy bear, brown fur, red bow tie", "type", "teddy bear", "color", "brown", "accessory", "red bow");
        Pin foundToy = createPin("found", "description", "Found brown stuffed bear with red ribbon around neck", "type", "stuffed animal", "color", "brown", "feature", "red ribbon");
        
        System.out.println("=== GEMINI CHILD TOY MATCH TEST ===");
        System.out.println("Lost: " + LostAndFoundMatcher.preprocessPin(lostToy));
        System.out.println("Found: " + LostAndFoundMatcher.preprocessPin(foundToy));
        boolean result = LostAndFoundMatcher.matchSync(lostToy, foundToy);
        System.out.println("Match result: " + result);
        System.out.println("===================================");
        
        assertTrue(result, "Teddy bear and stuffed bear with similar features should match");
    }
    
    @Test
    void testDocumentImportance() {
        // Test case 23: Important documents - should be specific
        Pin lostDoc = createPin("lost", "description", "Lost passport, blue cover, expires 2028", "type", "passport", "color", "blue", "expiry", "2028");
        Pin foundDoc = createPin("found", "description", "Found driver's license in blue wallet", "type", "driver license", "wallet_color", "blue");
        
        System.out.println("=== GEMINI DOCUMENT IMPORTANCE TEST ===");
        System.out.println("Lost: " + LostAndFoundMatcher.preprocessPin(lostDoc));
        System.out.println("Found: " + LostAndFoundMatcher.preprocessPin(foundDoc));
        boolean result = LostAndFoundMatcher.matchSync(lostDoc, foundDoc);
        System.out.println("Match result: " + result);
        System.out.println("=======================================");
        
        assertFalse(result, "Passport and driver's license should not match");
    }
    
    @Test
    void testPreprocessPin() {
        Pin pin = createPin("lost", "description", "Lost black phone", "color", "black");
        String result = LostAndFoundMatcher.preprocessPin(pin);
        assertTrue(result.contains("Category: lost"));
        assertTrue(result.contains("black"));
    }
    
    private Pin createPin(String category, String... tagPairs) {
        Map<String, String> tags = new HashMap<>();
        for (int i = 0; i < tagPairs.length; i += 2) {
            tags.put(tagPairs[i], tagPairs[i + 1]);
        }
        PGpoint location = new PGpoint(50.0, 19.9);
        return Pin.create(location, category, tags);
    }
} 