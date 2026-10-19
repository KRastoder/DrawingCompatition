package com.drawing_compatition.KRastoder.demo;

import com.drawing_compatition.KRastoder.comment.Comment;
import com.drawing_compatition.KRastoder.comment.CommentRepository;
import com.drawing_compatition.KRastoder.drawing.Drawing;
import com.drawing_compatition.KRastoder.drawing.DrawingRepository;
import com.drawing_compatition.KRastoder.user.User;
import com.drawing_compatition.KRastoder.user.UserRepository;
import com.drawing_compatition.KRastoder.vote.Vote;
import com.drawing_compatition.KRastoder.vote.VoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Optional development-only demo data. Enable with app.seed-demo-data=true.
 *
 * The seeder tops the database up to exactly ten accounts and one hundred
 * drawings, so it can be run repeatedly without creating duplicates and
 * without deleting anybody's existing work.
 */
@Component
@ConditionalOnProperty(prefix = "app", name = "seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final int TARGET_USERS = 10;
    private static final int TARGET_DRAWINGS = 100;
    private static final String DEMO_PASSWORD = "demo1234";

    private static final String[][] DEMO_USERS = {
            {"pixelpanda", "Pixel Panda", "I draw tiny animals and even tinier stars."},
            {"doodlebug", "Doodle Bug", "Professional doodler. Amateur everything else."},
            {"inkblot", "Ink Blot", "Mostly blobs, occasionally masterpieces."},
            {"sketchy", "Sketchy Sam", "Pencil first, questions later."},
            {"crayonkid", "Crayon Kid", "I never grew out of the big box of crayons."},
            {"metrocat", "Metro Cat", "City lights, sleepy cats, and MS Paint."},
            {"glitterpen", "Glitter Pen", "Everything is better with sparkles."},
            {"pixelwitch", "Pixel Witch", "Casting small spells with big pixels."},
            {"softpencil", "Soft Pencil", "Gentle lines, loud colours."},
            {"scribblefox", "Scribble Fox", "A fox with a stylus and no plan."}
    };

    private static final String[] DRAWING_TITLES = {
            "Midnight Doodle", "Tiny Dragon", "Cloud Castle", "Pixel Garden",
            "Space Snail", "Grumpy Star", "Neon Forest", "Sleepy Robot",
            "Rainy Window", "Cosmic Cat", "Dance Party", "Secret Treehouse",
            "Underwater Postcard", "Mystery Blob", "Sunset Mountains", "Pocket Universe",
            "Friendly Monster", "Lost Balloon", "Crystal Cave", "Retro Computer",
            "Bubble Tea Planet", "Moon Bunny", "Electric Jellyfish", "Skateboard Wizard",
            "Tiny Cafe", "Floating Island", "Dream Library", "Lava Lamp Life",
            "Starry Highway", "Paper Boat", "Disco Frog", "Garden Gnome",
            "Toasty Campfire", "Comet Rider", "Mermaid Lagoon", "Arcade Hero",
            "Cloud Whale", "Sunflower City", "Neon Dinosaur", "Pizza Planet"
    };

    private static final String[] DRAWING_NOTES = {
            "Made this while listening to the dial-up modem song.",
            "I think the perspective is a little cursed, but I love it.",
            "Drawn entirely with the mouse. My wrist hurts.",
            "This took way longer than it should have.",
            "Inspired by a very strange dream.",
            "Entry for the weekly doodle challenge.",
            "Do not ask what the creature is. It is doing its best.",
            "Three colours and a whole lot of enthusiasm.",
            "I found the airbrush tool and could not be stopped.",
            "My masterpiece. Probably. Maybe.",
            "The background started as a mistake and became the best part.",
            "Dedicated to everyone who still uses MS Paint."
    };

    private static final String[] COMMENTS = {
            "This is so cute! \u2665",
            "The colours are amazing.",
            "How did you do the background?",
            "I love this little guy.",
            "Voted! Please make more.",
            "This belongs in a museum. A tiny internet museum.",
            "The vibes are immaculate.",
            "I can hear the dial-up modem looking at this.",
            "So nostalgic. Instant favourite.",
            "The stars are my favourite part.",
            "This made my day!",
            "10/10 would doodle again."
    };

    private static final Color[] INK_COLORS = {
            new Color(0x111111), new Color(0x1d4ed8), new Color(0x9b0054),
            new Color(0x006b3c), new Color(0x7b2cbf), new Color(0xb45309),
            new Color(0x0080a8), new Color(0xd00000)
    };

    private static final Color[][] GRADIENTS = {
            {new Color(0x1a0b2e), new Color(0xff2d95)},
            {new Color(0x050518), new Color(0x1d4ed8)},
            {new Color(0x0b3d2e), new Color(0x7df9ff)},
            {new Color(0x3b0a0a), new Color(0xffb703)},
            {new Color(0xfff1f8), new Color(0x7df9ff)},
            {new Color(0x111111), new Color(0xffe66d)},
            {new Color(0x2b0a4a), new Color(0x00c2ff)},
            {new Color(0xfdf8ef), new Color(0xff9ecb)}
    };

    private final UserRepository users;
    private final DrawingRepository drawings;
    private final VoteRepository votes;
    private final CommentRepository comments;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;
    private final Random random = new Random(20100917L);

    public DemoDataSeeder(
            UserRepository users,
            DrawingRepository drawings,
            VoteRepository votes,
            CommentRepository comments,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbc
    ) {
        this.users = users;
        this.drawings = drawings;
        this.votes = votes;
        this.comments = comments;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        List<User> allUsers = new ArrayList<>(users.findAll());
        allUsers.sort(Comparator.comparing(User::getId));

        List<User> demoUsers = new ArrayList<>();
        for (String[] definition : DEMO_USERS) {
            users.findByUsername(definition[0]).ifPresent(demoUsers::add);
        }

        int usersAdded = 0;
        if (allUsers.size() < TARGET_USERS) {
            for (String[] definition : DEMO_USERS) {
                if (allUsers.size() >= TARGET_USERS) {
                    break;
                }
                if (users.existsByUsername(definition[0])) {
                    continue;
                }

                User user = new User(
                        definition[0],
                        definition[1],
                        passwordEncoder.encode(DEMO_PASSWORD)
                );
                user.setBio(definition[2]);
                user.setAvatar(generateAvatar(definition[1]), "image/png");
                user = users.save(user);

                allUsers.add(user);
                demoUsers.add(user);
                usersAdded++;
            }
        }

        if (demoUsers.isEmpty()) {
            demoUsers.addAll(allUsers);
        }

        long existingDrawings = drawings.count();
        int drawingsToCreate = (int) Math.max(0, TARGET_DRAWINGS - existingDrawings);
        int drawingsCreated = 0;
        List<Drawing> seededDrawings = new ArrayList<>();

        for (int index = 0; index < drawingsToCreate; index++) {
            User owner = demoUsers.get(random.nextInt(demoUsers.size()));
            String title = DRAWING_TITLES[random.nextInt(DRAWING_TITLES.length)];
            String note = DRAWING_NOTES[random.nextInt(DRAWING_NOTES.length)];

            Drawing drawing = new Drawing(
                    owner,
                    title + "\n" + note,
                    generateArtwork(title),
                    "image/png"
            );
            drawing = drawings.save(drawing);

            Instant createdAt = randomCreatedAt();
            jdbc.update(
                    "update drawings set created_at = ?, updated_at = ? where id = ?",
                    java.sql.Timestamp.from(createdAt),
                    java.sql.Timestamp.from(createdAt),
                    drawing.getId()
            );

            seededDrawings.add(drawing);
            drawingsCreated++;
        }

        int votesCreated = seedVotes(seededDrawings, allUsers);
        int commentsCreated = seedComments(seededDrawings, allUsers);

        log.info(
                "Demo data ready: {} user(s) added, {} drawing(s) added, {} vote(s), {} comment(s). " +
                        "Database now has {} users and {} drawings. Login demo users with password '{}'.",
                usersAdded,
                drawingsCreated,
                votesCreated,
                commentsCreated,
                users.count(),
                drawings.count(),
                DEMO_PASSWORD
        );
    }

    private Instant randomCreatedAt() {
        // Keep a healthy amount of drawings on the current day so the
        // day filter is useful, then spread the rest over the last month.
        if (random.nextInt(100) < 22) {
            long minutesAgo = 5L + random.nextInt(12 * 60);
            return Instant.now().minus(Duration.ofMinutes(minutesAgo));
        }
        long hoursAgo = 24L + random.nextInt(27 * 24);
        return Instant.now().minus(Duration.ofHours(hoursAgo));
    }

    private int seedVotes(List<Drawing> seededDrawings, List<User> allUsers) {
        int created = 0;
        for (Drawing drawing : seededDrawings) {
            int voterCount = random.nextInt(8);
            Set<Long> used = new HashSet<>();
            used.add(drawing.getUser().getId());

            for (int index = 0; index < voterCount; index++) {
                User voter = allUsers.get(random.nextInt(allUsers.size()));
                if (!used.add(voter.getId())) {
                    continue;
                }
                votes.save(new Vote(drawing, voter));
                created++;
            }
        }
        return created;
    }

    private int seedComments(List<Drawing> seededDrawings, List<User> allUsers) {
        int created = 0;
        for (Drawing drawing : seededDrawings) {
            int commentCount = random.nextInt(4);
            for (int index = 0; index < commentCount; index++) {
                User author = allUsers.get(random.nextInt(allUsers.size()));
                String body = COMMENTS[random.nextInt(COMMENTS.length)];
                comments.save(new Comment(drawing, author, body));
                created++;
            }
        }
        return created;
    }

    private byte[] generateArtwork(String title) {
        int width = 1000;
        int height = 700;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            Color[] gradient = GRADIENTS[random.nextInt(GRADIENTS.length)];
            graphics.setPaint(new GradientPaint(0, 0, gradient[0], width, height, gradient[1]));
            graphics.fillRect(0, 0, width, height);

            // Translucent blobs.
            for (int index = 0; index < 22; index++) {
                Color colour = INK_COLORS[random.nextInt(INK_COLORS.length)];
                graphics.setColor(new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), 35 + random.nextInt(70)));
                int size = 40 + random.nextInt(220);
                graphics.fill(new Ellipse2D.Double(
                        random.nextInt(width),
                        random.nextInt(height),
                        size,
                        size * (0.5 + random.nextDouble())
                ));
            }

            // Big doodle in the middle.
            Color ink = INK_COLORS[random.nextInt(INK_COLORS.length)];
            graphics.setColor(ink);
            graphics.setStroke(new BasicStroke(3 + random.nextInt(8), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int centreX = width / 2 + random.nextInt(180) - 90;
            int centreY = height / 2 + random.nextInt(120) - 60;
            int radius = 70 + random.nextInt(130);

            switch (random.nextInt(5)) {
                case 0 -> drawFace(graphics, centreX, centreY, radius, ink);
                case 1 -> drawSpiral(graphics, centreX, centreY, radius);
                case 2 -> drawMountainScene(graphics, width, height, ink);
                case 3 -> drawStarField(graphics, width, height, ink);
                default -> drawSquiggleMonster(graphics, centreX, centreY, radius);
            }

            // Scribbles around the edges.
            for (int index = 0; index < 7; index++) {
                Path2D path = new Path2D.Double();
                double x = random.nextInt(width);
                double y = random.nextInt(height);
                path.moveTo(x, y);
                for (int step = 0; step < 5; step++) {
                    path.curveTo(
                            x + random.nextInt(120) - 60, y + random.nextInt(120) - 60,
                            x + random.nextInt(160) - 80, y + random.nextInt(160) - 80,
                            x + random.nextInt(180) - 90, y + random.nextInt(180) - 90
                    );
                }
                graphics.setColor(new Color(
                        ink.getRed(), ink.getGreen(), ink.getBlue(), 80 + random.nextInt(100)
                ));
                graphics.setStroke(new BasicStroke(1 + random.nextInt(4), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                graphics.draw(path);
            }

            // Little stars and sparkles.
            for (int index = 0; index < 26; index++) {
                drawSparkle(graphics, random.nextInt(width), random.nextInt(height), 2 + random.nextInt(7));
            }

            // Footer strip with the title.
            graphics.setColor(new Color(0, 0, 0, 150));
            graphics.fillRect(0, height - 46, width, 46);
            graphics.setColor(new Color(0xffe66d));
            graphics.setFont(graphics.getFont().deriveFont(java.awt.Font.BOLD, 20f));
            graphics.drawString(title, 18, height - 16);
            graphics.setColor(Color.WHITE);
            graphics.setFont(graphics.getFont().deriveFont(java.awt.Font.PLAIN, 12f));
            graphics.drawString("the drawing competition \u00b7 demo doodle", width - 260, height - 16);

            return toPng(image);
        } finally {
            graphics.dispose();
        }
    }

    private void drawFace(Graphics2D graphics, int cx, int cy, int radius, Color ink) {
        graphics.setStroke(new BasicStroke(7, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(new Ellipse2D.Double(cx - radius, cy - radius, radius * 2.0, radius * 2.0));

        graphics.fill(new Ellipse2D.Double(cx - radius * 0.45, cy - radius * 0.25, radius * 0.18, radius * 0.22));
        graphics.fill(new Ellipse2D.Double(cx + radius * 0.27, cy - radius * 0.25, radius * 0.18, radius * 0.22));

        Path2D smile = new Path2D.Double();
        smile.moveTo(cx - radius * 0.45, cy + radius * 0.25);
        smile.quadTo(cx, cy + radius * 0.75, cx + radius * 0.45, cy + radius * 0.25);
        graphics.draw(smile);

        graphics.setColor(new Color(ink.getRed(), ink.getGreen(), ink.getBlue(), 90));
        graphics.fill(new Ellipse2D.Double(cx - radius * 0.7, cy + radius * 0.15, radius * 0.3, radius * 0.18));
        graphics.fill(new Ellipse2D.Double(cx + radius * 0.4, cy + radius * 0.15, radius * 0.3, radius * 0.18));
    }

    private void drawSpiral(Graphics2D graphics, int cx, int cy, int radius) {
        Path2D spiral = new Path2D.Double();
        for (int angle = 0; angle < 720; angle += 6) {
            double r = radius * angle / 720.0;
            double x = cx + Math.cos(Math.toRadians(angle)) * r;
            double y = cy + Math.sin(Math.toRadians(angle)) * r;
            if (angle == 0) {
                spiral.moveTo(x, y);
            } else {
                spiral.lineTo(x, y);
            }
        }
        graphics.draw(spiral);
    }

    private void drawMountainScene(Graphics2D graphics, int width, int height, Color ink) {
        Path2D mountains = new Path2D.Double();
        mountains.moveTo(90, height - 160);
        mountains.lineTo(300, height - 430);
        mountains.lineTo(470, height - 230);
        mountains.lineTo(650, height - 500);
        mountains.lineTo(900, height - 160);
        graphics.draw(mountains);

        graphics.fill(new Ellipse2D.Double(width - 250, 90, 90, 90));
        graphics.setColor(new Color(ink.getRed(), ink.getGreen(), ink.getBlue(), 120));
        graphics.fill(new Ellipse2D.Double(width - 265, 75, 120, 120));
    }

    private void drawStarField(Graphics2D graphics, int width, int height, Color ink) {
        graphics.setColor(new Color(255, 255, 255, 220));
        for (int index = 0; index < 80; index++) {
            int size = 2 + random.nextInt(5);
            graphics.fillRect(random.nextInt(width), random.nextInt(height), size, size);
        }
        graphics.setColor(ink);
        drawSparkle(graphics, width / 2, height / 2 - 30, 60);
    }

    private void drawSquiggleMonster(Graphics2D graphics, int cx, int cy, int radius) {
        Path2D body = new Path2D.Double();
        body.moveTo(cx - radius, cy + radius * 0.6);
        for (int index = 0; index < 8; index++) {
            double x = cx - radius + (radius * 2.0 * index / 8.0);
            double y = cy + (index % 2 == 0 ? radius * 0.2 : radius * 0.9);
            body.quadTo(x + radius / 8.0, y - radius * 0.4, x, y);
        }
        graphics.draw(body);
        graphics.fill(new Ellipse2D.Double(cx - radius * 0.35, cy - radius * 0.2, radius * 0.18, radius * 0.22));
        graphics.fill(new Ellipse2D.Double(cx + radius * 0.18, cy - radius * 0.2, radius * 0.18, radius * 0.22));
    }

    private void drawSparkle(Graphics2D graphics, int x, int y, int size) {
        Color previous = graphics.getColor();
        graphics.setColor(new Color(255, 255, 255, 210));
        Path2D star = new Path2D.Double();
        star.moveTo(x, y - size);
        star.lineTo(x + size * 0.3, y - size * 0.3);
        star.lineTo(x + size, y);
        star.lineTo(x + size * 0.3, y + size * 0.3);
        star.lineTo(x, y + size);
        star.lineTo(x - size * 0.3, y + size * 0.3);
        star.lineTo(x - size, y);
        star.lineTo(x - size * 0.3, y - size * 0.3);
        star.closePath();
        graphics.fill(star);
        graphics.setColor(previous);
    }

    private byte[] generateAvatar(String displayName) {
        int size = 160;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color[] gradient = GRADIENTS[random.nextInt(GRADIENTS.length)];
            graphics.setPaint(new GradientPaint(0, 0, gradient[0], size, size, gradient[1]));
            graphics.fillRect(0, 0, size, size);

            Color ink = INK_COLORS[random.nextInt(INK_COLORS.length)];
            graphics.setColor(ink);
            graphics.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int radius = 42 + random.nextInt(18);
            drawFace(graphics, size / 2, size / 2, radius, ink);

            for (int index = 0; index < 8; index++) {
                drawSparkle(graphics, random.nextInt(size), random.nextInt(size), 2 + random.nextInt(4));
            }

            graphics.setColor(new Color(0, 0, 0, 140));
            graphics.fillRect(0, size - 22, size, 22);
            graphics.setColor(Color.WHITE);
            graphics.setFont(graphics.getFont().deriveFont(java.awt.Font.BOLD, 10f));
            String label = displayName.length() > 20 ? displayName.substring(0, 20) : displayName;
            graphics.drawString(label, 6, size - 8);

            return toPng(image);
        } finally {
            graphics.dispose();
        }
    }

    private byte[] toPng(BufferedImage image) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate demo artwork.", exception);
        }
    }
}
