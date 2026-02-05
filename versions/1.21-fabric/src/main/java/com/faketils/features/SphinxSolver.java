package com.faketils.features;

import com.faketils.config.Config;
import com.faketils.utils.Utils;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class SphinxSolver {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Random RANDOM = new Random();

    private static final List<SphinxQuestion> questions = new ArrayList<>();
    private static String pendingAnswer = null;

    public static void init() {
        addQuestions();

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            onChat(message);
        });
    }

    private static void onChat(Text message) {
        if (!Utils.isInSkyblock()) return;
        if (!Config.INSTANCE.sphinxSolver) return;

        String raw = removeFormatting(message.getString()).trim();

        if (pendingAnswer != null) {
            autoClickIfAnswer(message);
            return;
        }

        for (SphinxQuestion question : questions) {
            if (question.question.equalsIgnoreCase(raw)) {
                pendingAnswer = question.answer;
                Utils.log("§aCorrect answer: " + question.answer);
                return;
            }
        }
    }

    private static void autoClickIfAnswer(Text component) {
        if (pendingAnswer == null) return;
        scan(component);
    }

    private static void scan(Text text) {
        String cleanText = removeFormatting(text.getString()).trim();
        Style style = text.getStyle();
        ClickEvent clickEvent = style.getClickEvent();

        if (clickEvent != null
                && clickEvent.getAction() == ClickEvent.Action.RUN_COMMAND
                && cleanText.toLowerCase().contains(pendingAnswer.toLowerCase())) {

            long delay = 1000L + RANDOM.nextInt(1000);

            new Thread(() -> {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {}
                mc.execute(() -> {
                    if (mc.currentScreen != null) {
                        mc.currentScreen.handleTextClick(text.getStyle());
                        pendingAnswer = null;
                    }
                });
            }).start();
        }

        for (Text sibling : text.getSiblings()) {
            scan(sibling);
        }
    }

    private static String removeFormatting(String input) {
        return input.replaceAll("§.", "");
    }

    private static void addQuestions() {
        questions.add(new SphinxQuestion("Which of these is NOT a pet?", "Slime"));
        questions.add(new SphinxQuestion("What type of mob is exclusive to the Fishing Festival?", "Shark"));
        questions.add(new SphinxQuestion("Where is Trevor the Trapper found?", "Mushroom Desert"));
        questions.add(new SphinxQuestion("Who helps you apply Rod Parts?", "Roddy"));
        questions.add(new SphinxQuestion("Which type of Gemstone has the lowest Breaking Power?", "Ruby"));
        questions.add(new SphinxQuestion("Which item rarity comes after Mythic?", "Divine"));
        questions.add(new SphinxQuestion("How do you obtain the Dark Purple Dye?", "Dark Auction"));
        questions.add(new SphinxQuestion("Who runs the Chocolate Factory?", "Hoppity"));
        questions.add(new SphinxQuestion("How many floors are there in The Catacombs?", "7"));
        questions.add(new SphinxQuestion("What is the first type of slayer Maddox offers?", "Zombie"));
        questions.add(new SphinxQuestion("What item do you use to kill Pests?", "Vacuum"));
        questions.add(new SphinxQuestion("Who owns the Gold Essence Shop?", "Marigold"));
        questions.add(new SphinxQuestion("Which of these is NOT a type of Gemstone?", "Prismite"));
        questions.add(new SphinxQuestion("What does Junker Joel collect?", "Junk"));
        questions.add(new SphinxQuestion("Where is the Titanoboa found?", "Backwater Bayou"));
    }

    private record SphinxQuestion(String question, String answer) {}
}