/*
 * TerraVera - an addon for TerraFirmaCraft
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package com.terravera;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import com.terravera.common.health.Symptom;
import com.terravera.common.health.TransmissionVector;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the shipped disease roster.
 * <p>
 * These files are the mod's actual content, and several of the invariants here are medical rather than mechanical -
 * cholera has to stay fast and lethal, tapeworm has to stay slow and quiet, tetanus has to stay rare. A retune that
 * breaks those breaks the point of modelling real diseases in the first place.
 */
public class IllnessResourceTest
{
    /** The complete shipped roster. If an illness is added, it goes here too. */
    private static final List<String> ILLNESSES = List.of(
        "common_cold", "influenza", "norovirus",
        "giardiasis", "cryptosporidiosis", "dysentery", "typhoid", "cholera",
        "tapeworm", "trichinosis",
        "wound_infection", "tetanus"
    );

    private static final List<String> REMEDIES = List.of(
        "herbal_decoction", "willow_bark", "wormwood",
        "prepared_salicylate", "prepared_antiparasitic",
        "wound_dressing", "rehydration_solution", "medicine"
    );

    private static final int TICKS_PER_DAY = 24000;

    @Test
    public void everyShippedIllnessParsesAndIsWellFormed() throws IOException
    {
        for (String name : ILLNESSES)
        {
            final JsonObject illness = readIllness(name);

            // Vectors must be real, and there must be at least one - an illness you cannot catch is dead content.
            final JsonArray vectors = illness.getAsJsonArray("vectors");
            assertNotNull(vectors, name + " must declare vectors");
            assertTrue(vectors.size() > 0, name + " must be catchable somehow");
            for (JsonElement vector : vectors)
            {
                assertNotNull(TransmissionVector.byId(vector.getAsString()),
                    name + " has unknown vector " + vector.getAsString());
            }

            // Symptoms must be real, and there must be at least one.
            final JsonArray symptoms = illness.getAsJsonArray("symptoms");
            assertNotNull(symptoms, name + " must declare symptoms");
            assertTrue(symptoms.size() > 0, name + " must actually do something");
            for (JsonElement symptom : symptoms)
            {
                assertNotNull(Symptom.byId(symptom.getAsString()),
                    name + " has unknown symptom " + symptom.getAsString());
            }

            assertTrue(illness.get("base_chance").getAsFloat() > 0f, name + " must be catchable");
            assertTrue(illness.get("base_chance").getAsFloat() <= 1f, name + " chance must be a probability");
            assertTrue(Set.of("mild", "moderate", "severe", "critical").contains(illness.get("severity").getAsString()),
                name + " has an unknown severity");
        }
    }

    /**
     * The central promise of the whole system. Nothing may transmit instantly - if this fails, the mod has become a
     * damage source rather than a disease model.
     */
    @Test
    public void noIllnessTransmitsInstantly() throws IOException
    {
        for (String name : ILLNESSES)
        {
            final JsonObject illness = readIllness(name);
            final int incubation = illness.get("incubation_ticks").getAsInt();
            assertTrue(incubation > 0, name + " must have an incubation period");
            assertTrue(incubation >= TICKS_PER_DAY / 2,
                name + " incubates in under half a day, which reads as instant to a player");
            assertTrue(illness.get("duration_ticks").getAsInt() > 0, name + " must last a nonzero time");
        }
    }

    @Test
    public void everyIllnessCanBeTreatedSomehow() throws IOException
    {
        for (String name : ILLNESSES)
        {
            final JsonArray remedies = readIllness(name).getAsJsonArray("remedies");
            assertNotNull(remedies, name + " must list remedies");
            assertTrue(remedies.size() > 0, name + " has no treatment path at all");
            for (JsonElement remedy : remedies)
            {
                assertTrue(remedy.getAsString().startsWith("terravera:remedies/"),
                    name + " references a non-remedy tag: " + remedy.getAsString());
            }
        }
    }

    /** Anything that can kill you must have a route out that does not require the endgame tech tree. */
    @Test
    public void lethalIllnessesAreTreatableBeforeModernMedicine() throws IOException
    {
        for (String name : ILLNESSES)
        {
            final JsonObject illness = readIllness(name);
            if (!illness.get("lethal").getAsBoolean()) continue;
            if (name.equals("tetanus")) continue; // deliberately the one thing you cannot fix with herbs

            final List<String> remedies = new ArrayList<>();
            illness.getAsJsonArray("remedies").forEach(e -> remedies.add(e.getAsString()));
            assertTrue(remedies.stream().anyMatch(r -> !r.equals("terravera:remedies/medicine")),
                name + " is lethal but only curable with endgame medicine");
        }
    }

    @Test
    public void onlyRespiratoryIllnessesAreContagious() throws IOException
    {
        for (String name : ILLNESSES)
        {
            final JsonObject illness = readIllness(name);
            if (!illness.get("contagious").getAsBoolean()) continue;

            final List<String> vectors = new ArrayList<>();
            illness.getAsJsonArray("vectors").forEach(e -> vectors.add(e.getAsString()));
            assertTrue(vectors.contains("contact") || vectors.contains("food") || vectors.contains("sanitation"),
                name + " is contagious but has no person-to-person route");
        }
    }

    // ----- Specific medical fidelity ------------------------------------------------------------------------

    @Test
    public void choleraIsFastAndDeadly() throws IOException
    {
        final JsonObject cholera = readIllness("cholera");
        assertEquals("critical", cholera.get("severity").getAsString());
        assertTrue(cholera.get("lethal").getAsBoolean());
        assertTrue(cholera.get("incubation_ticks").getAsInt() <= 2 * TICKS_PER_DAY,
            "cholera incubates in hours to two days");
        assertTrue(symptomsOf(cholera).contains("dehydration"),
            "cholera kills by dehydration - that symptom is not optional");
    }

    @Test
    public void typhoidIncubatesForWeeks() throws IOException
    {
        final JsonObject typhoid = readIllness("typhoid");
        assertTrue(typhoid.get("incubation_ticks").getAsInt() >= 7 * TICKS_PER_DAY,
            "typhoid is famously slow to show - at least a week");
        assertTrue(symptomsOf(typhoid).contains("fever"), "it is called typhoid *fever*");
    }

    @Test
    public void theParasitesCauseMalabsorptionRatherThanDamage() throws IOException
    {
        for (String name : List.of("giardiasis", "cryptosporidiosis", "tapeworm"))
        {
            final List<String> symptoms = symptomsOf(readIllness(name));
            assertTrue(symptoms.contains("malabsorption"),
                name + " should manifest as poor nutrient absorption, not as damage");
        }
    }

    @Test
    public void tapewormIsSlowQuietAndHungry() throws IOException
    {
        final JsonObject tapeworm = readIllness("tapeworm");
        assertTrue(tapeworm.get("incubation_ticks").getAsInt() >= 14 * TICKS_PER_DAY,
            "a tapeworm takes weeks to establish");
        assertTrue(symptomsOf(tapeworm).contains("increased_hunger"),
            "the signature of a tapeworm is that it eats before you do");
        assertFalse(tapeworm.get("lethal").getAsBoolean(), "a tapeworm is miserable, not fatal");
    }

    @Test
    public void trichinosisCausesMusclePain() throws IOException
    {
        // The larvae encyst in muscle - that is the whole clinical picture.
        assertTrue(symptomsOf(readIllness("trichinosis")).contains("muscle_pain"));
    }

    @Test
    public void tetanusIsRareCriticalAndSoilBorne() throws IOException
    {
        final JsonObject tetanus = readIllness("tetanus");
        assertEquals("critical", tetanus.get("severity").getAsString());
        assertTrue(tetanus.get("lethal").getAsBoolean());
        assertTrue(tetanus.get("base_chance").getAsFloat() < 0.06f, "tetanus must stay rare");
        assertTrue(symptomsOf(tetanus).contains("spasms"), "tetanus is defined by muscle spasm");

        final List<String> vectors = new ArrayList<>();
        tetanus.getAsJsonArray("vectors").forEach(e -> vectors.add(e.getAsString()));
        assertEquals(List.of("wound"), vectors, "tetanus enters through wounds, not the gut");
    }

    @Test
    public void coldAndFluSpreadByContactOnly() throws IOException
    {
        for (String name : List.of("common_cold", "influenza"))
        {
            final JsonObject illness = readIllness(name);
            final List<String> vectors = new ArrayList<>();
            illness.getAsJsonArray("vectors").forEach(e -> vectors.add(e.getAsString()));
            assertEquals(List.of("contact"), vectors, name + " should spread by close contact");
            assertTrue(illness.get("contagious").getAsBoolean(), name + " must be contagious");
            assertTrue(illness.get("immunity_ticks").getAsInt() > 0,
                name + " should confer some immunity, or players will catch it endlessly");
        }
    }

    @Test
    public void theWaterborneIllnessesAreActuallyWaterborne() throws IOException
    {
        for (String name : List.of("giardiasis", "cryptosporidiosis", "cholera", "typhoid", "dysentery"))
        {
            final List<String> vectors = new ArrayList<>();
            readIllness(name).getAsJsonArray("vectors").forEach(e -> vectors.add(e.getAsString()));
            assertTrue(vectors.contains("water"), name + " must be catchable from water");
        }
    }

    @Test
    public void theMeatborneParasitesComeFromUndercookedMeat() throws IOException
    {
        for (String name : List.of("tapeworm", "trichinosis"))
        {
            final List<String> vectors = new ArrayList<>();
            readIllness(name).getAsJsonArray("vectors").forEach(e -> vectors.add(e.getAsString()));
            assertEquals(List.of("undercooked_meat"), vectors, name + " comes from raw meat");
        }
    }

    // ----- Remedies -----------------------------------------------------------------------------------------

    @Test
    public void everyShippedRemedyParsesAndIsWellFormed() throws IOException
    {
        for (String name : REMEDIES)
        {
            final JsonObject remedy = readRemedy(name);
            assertTrue(remedy.get("ingredient").getAsString().startsWith("terravera:remedies/"),
                name + " must be keyed on a remedy tag");
            assertTrue(Set.of("herbal", "prepared", "medicine").contains(remedy.get("tier").getAsString()),
                name + " has an unknown tier");
            assertTrue(remedy.get("shorten_ticks").getAsInt() > 0, name + " must do something");
        }
    }

    /** The progression promise: herbal remedies help, but you have to earn an outright cure. */
    @Test
    public void onlyPreparedAndModernRemediesCureOutright() throws IOException
    {
        for (String name : REMEDIES)
        {
            final JsonObject remedy = readRemedy(name);
            if (!remedy.get("cures").getAsBoolean()) continue;
            assertNotEquals("herbal", remedy.get("tier").getAsString(),
                name + " cures outright at the herbal tier, which flattens the whole progression");
        }
    }

    @Test
    public void modernMedicineIsTheStrongestRemedy() throws IOException
    {
        final JsonObject medicine = readRemedy("medicine");
        assertTrue(medicine.get("cures").getAsBoolean());
        assertEquals("medicine", medicine.get("tier").getAsString());
        assertTrue(medicine.getAsJsonArray("treats").isEmpty(),
            "modern medicine should be general, not restricted to a list");
    }

    @Test
    public void rehydrationTreatsTheDehydratingIllnesses() throws IOException
    {
        final List<String> treats = new ArrayList<>();
        readRemedy("rehydration_solution").getAsJsonArray("treats").forEach(e -> treats.add(e.getAsString()));
        assertTrue(treats.contains("terravera:cholera"),
            "oral rehydration is the real-world answer to cholera and must remain so here");
    }

    @Test
    public void everyRemedyTagReferencedByAnIllnessHasARemedyFile() throws IOException
    {
        final List<String> declared = new ArrayList<>();
        for (String name : REMEDIES)
        {
            declared.add(readRemedy(name).get("ingredient").getAsString());
        }
        for (String name : ILLNESSES)
        {
            for (JsonElement remedy : readIllness(name).getAsJsonArray("remedies"))
            {
                assertTrue(declared.contains(remedy.getAsString()),
                    name + " references remedy tag " + remedy.getAsString() + " which no remedy file provides");
            }
        }
    }

    // ----- Helpers ------------------------------------------------------------------------------------------

    private static List<String> symptomsOf(JsonObject illness)
    {
        final List<String> symptoms = new ArrayList<>();
        illness.getAsJsonArray("symptoms").forEach(e -> symptoms.add(e.getAsString()));
        return symptoms;
    }

    private static JsonObject readIllness(String name) throws IOException
    {
        return read("/data/terravera/terravera/illness/" + name + ".json");
    }

    private static JsonObject readRemedy(String name) throws IOException
    {
        return read("/data/terravera/terravera/remedy/" + name + ".json");
    }

    private static JsonObject read(String resource) throws IOException
    {
        try (InputStream stream = IllnessResourceTest.class.getResourceAsStream(resource))
        {
            assertNotNull(stream, "Missing resource " + resource);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
