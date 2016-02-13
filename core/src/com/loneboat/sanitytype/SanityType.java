package com.loneboat.sanitytype;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

import java.io.IOException;
import java.math.BigInteger;

public class SanityType extends Game {

    private static final float PPE = 100;

	SpriteBatch batch;
	BitmapFont font;

    private Array<Array<String>> chooser;

	private Array<String> oneSyllableVerbs;
	private Array<String> twoSyllableVerbs;
	private Array<String> threeSyllableVerbs;
	private Array<String> fourSyllableVerbs;

    private int level_index;
    private Level active;
    private Array<Level> levels;

    private Stage words;
    private Stage input;
    private Stage pauseStage;

    private ShapeRenderer renderer;

    private World world;

    private WordSprite focus;
    private TextField field;

    private float timer = 0;
    private float speed = 2;

    private BigInteger score = BigInteger.valueOf(250001);
    private double multiplier = 1.0;

    private GameState gs;
    public enum GameState {
        RUN, PAUSE, RESUME
    }

    @Override
	public void create () {
		Gdx.app.setLogLevel(Application.LOG_DEBUG);
		font = new BitmapFont();
        batch = new SpriteBatch();
        gs = GameState.RUN;

        words = new Stage();
        input = new Stage();

        pauseStage = new Stage(new ExtendViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        createPauseUI();

        renderer = new ShapeRenderer();

        world = new World(new Vector2(0, 0), true);

        chooser = new Array<>();

		oneSyllableVerbs = new Array<>();
		twoSyllableVerbs = new Array<>();
		threeSyllableVerbs = new Array<>();
		fourSyllableVerbs = new Array<>();

		String[] words = Gdx.files.internal("1sylwords.txt").readString().split(",");
        oneSyllableVerbs.addAll(words);
        chooser.add(oneSyllableVerbs);

        words = Gdx.files.internal("2sylwords.txt").readString().split(",");
        twoSyllableVerbs.addAll(words);
        chooser.add(twoSyllableVerbs);

        words = Gdx.files.internal("3sylwords.txt").readString().split(",");
        threeSyllableVerbs.addAll(words);
        chooser.add(threeSyllableVerbs);

        words = Gdx.files.internal("4sylwords.txt").readString().split(",");
        fourSyllableVerbs.addAll(words);
        chooser.add(fourSyllableVerbs);

        field = new TextField("", new Skin(Gdx.files.internal("ui/uiskin.json")));
        field.setPosition(0, 0);
        field.setSize(Gdx.graphics.getWidth(), 50);
        field.setAlignment(Align.center);
        input.addActor(field);

        // Load all the levels.
        levels = new Array<Level>();
        try {
            XmlReader reader = new XmlReader();
            XmlReader.Element root = reader.parse(Gdx.files.internal("stages.xml"));
            Array<XmlReader.Element> stages = root.getChildrenByName("stage");
            int x = 1;
            for(XmlReader.Element child : stages) {
                Level level = new Level();
                level.setCode(child.get("level_code"));
                level.setDifficulty(child.getInt("difficulty"));
                level.setMinSpeed(child.getFloat("minimumSpeed"));
                level.setMaxSpeed(child.getFloat("maximumSpeed"));
                level.setHasBoss(child.getBoolean("hasBoss"));
                level.setPassingScore(child.getInt("passingScore"));
                level.setWordFallSpeed(child.getFloat("wordFallSpeed"));
                level.setQuickCode(x + "");
                levels.add(level);
                x++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
               if(active != null)
                   active.spawnLevelCode();
            }
        }, 0, 1);

        setLevel(0);
        Gdx.input.setInputProcessor(input);
        input.setKeyboardFocus(field);
	}

    public void createPauseUI() {
        Skin skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        Label pauseLabel = new Label("Game is Paused.", skin);
        pauseLabel.setPosition(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2, Align.center);
        pauseStage.addActor(pauseLabel);

        TextButton exitButton = new TextButton("I've lost my sanity", skin);
        exitButton.setPosition(Gdx.graphics.getWidth() / 2, pauseLabel.getY() - exitButton.getHeight(), Align.center);
        pauseStage.addActor(exitButton);

        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

    }

	@Override
	public void render () {
        switch(gs) {
            case RUN:
                Gdx.gl.glClearColor(0, 0, 0, 1);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                if (active == null) {
                    Gdx.graphics.setTitle(Gdx.graphics.getFramesPerSecond() + " " + Gdx.input.getX() + " , " + Gdx.input.getY() + " (!Loading Level!)");
                    return;
                }
                Gdx.graphics.setTitle(Gdx.graphics.getFramesPerSecond() + " " + Gdx.input.getX() + " , " + Gdx.input.getY());

                // Add words based on timer.
                timer += Gdx.graphics.getDeltaTime();
                if (timer >= speed) {
                    addWordSprite();
                    timer = 0;
                }

                if (active.isPassable())
                    advanceLevel();

                words.act();
                words.draw();

                input.act();
                input.draw();

                if (active != null) {
                    if (words.getActors().size > 0)
                        focus = (WordSprite) words.getActors().first();
                    else
                        focus = addWordSprite();

                    if(focus != null) {
                        renderer.begin(ShapeRenderer.ShapeType.Filled);
                        renderer.setColor(getColorFromHeight(focus));
                        renderer.rectLine(
                                0,
                                focus.body.getPosition().y - (focus.text.height / 2),
                                focus.body.getPosition().x - 5,
                                focus.body.getPosition().y - (focus.text.height / 2),
                                focus.text.height
                        );
                        renderer.rectLine(
                                focus.body.getPosition().x + (focus.text.width + 5),
                                focus.body.getPosition().y - (focus.text.height / 2),
                                Gdx.graphics.getWidth(),
                                focus.body.getPosition().y - (focus.text.height / 2),
                                focus.text.height
                        );
                        renderer.setColor(Color.GRAY);
                        renderer.rect(0, Gdx.graphics.getHeight() - 30, Gdx.graphics.getWidth(), 30);
                        renderer.end();
                    }

                    batch.begin();
                    GlyphLayout gl = new GlyphLayout(font, "Total Score: " + score.intValue());
                    font.draw(batch, gl, 5, 590);
                    // formula: (value * multiplier) * active.getDifficulty()
                    font.draw(batch,
                            "[" + focus.getName().toUpperCase() + "]'s worth: (" + focus.getValue() + " * " + multiplier + ") * " + active.getDifficulty() + " = " + focus.getPossibleScore(),
                            gl.width + 15, 590);
                    batch.end();

                    if(Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY)) {
                        int length = field.getText().length();
                        if(length > 0 && length <= focus.getName().length()) {
                            correctChar(length - 1);
                            if(checkFocus(length - 1)) {
                                if(focus.getName().equalsIgnoreCase(field.getText().toLowerCase())) {
                                    focus.destroy();
                                    addScore();
                                    decSpeed(0.025f);
                                    active.clampSpeed();
                                    multiplier += 0.5;
                                }
                            }
                        }
                    }

                    if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                        setState(GameState.PAUSE);
                        Gdx.input.setInputProcessor(pauseStage);
                    }
                }

                world.step(1/60f, 6, 2);
                break;
            case PAUSE:
                Gdx.gl.glClearColor(0, 0, 0, 1);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                Gdx.graphics.setTitle(Gdx.graphics.getFramesPerSecond() + " Game Paused.");

                batch.begin();
                font.draw(batch, "Current Score: " + score.intValue(), 5, 585);
                font.draw(batch, "Current Multiplier: " + multiplier, 5, 565);
                font.draw(batch, "Current Speed: " + speed, 5, 545);
                font.draw(batch, "Current Level: " + active.getQuickCode(), 5, 525);
                batch.end();

                pauseStage.act(Gdx.graphics.getDeltaTime());
                pauseStage.draw();

                if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                    setState(GameState.RUN);
                    Gdx.input.setInputProcessor(input);
                    input.setKeyboardFocus(field);
                }
                break;
            case RESUME:
                break;
        }
	}

    @Override
    public void resize(int width, int height) {
        pauseStage.getViewport().update(width, height);
    }

    public void correctChar(int index) {
        char[] f_chr = focus.getName().toCharArray();
        char[] field_chr = field.getText().toCharArray();
        if(Character.toLowerCase(f_chr[index]) != Character.toLowerCase(field_chr[index])) {
            removeScore(100);
            multiplier = 1;
            active.clampSpeed();
            incSpeed(0.05f);
        }
    }

    public boolean checkFocus(int index) {
        char[] f_chr = focus.getName().toCharArray();
        char[] field_chr = field.getText().toCharArray();
        return (Character.toLowerCase(f_chr[index]) == Character.toLowerCase(field_chr[index])) && (f_chr.length == field_chr.length);
    }

    public WordSprite addWordSprite() {
        WordSprite ws = new WordSprite();
        ws.setValue(1000);
        words.addActor(ws);
        return ws;
    }

    public void addScore() {
        double result = (focus.getValue() * multiplier) * active.getDifficulty();
        score = score.add(BigInteger.valueOf((long) result));
    }

    public void removeScore(int value) {
        score = score.subtract(BigInteger.valueOf(value));
    }

    public void incSpeed(float speed) {
        this.speed -= speed;
    }

    public void decSpeed(float speed) {
        this.speed += speed;
    }

    public Level getLevel(String id) {
        for(Level level : levels)
            if(level.getCode().equals(id))
                return level;
        return null;
    }

    public void setLevel(int index) {
        if(index < levels.size) {
            this.active = levels.get(index);
            level_index = index;
        }
    }

    public void advanceLevel() {
        if(level_index < levels.size) {
            this.active = levels.get(level_index + 1);
            level_index++;
        }
    }

	public void log(String message) {
		Gdx.app.debug("DebugOut", message);
	}

    public void setState(GameState state) {
        this.gs = state;
    }

    public Color getColorFromHeight(WordSprite ws) {
        float height = ws.body.getPosition().y;
        float windowHeight = Gdx.graphics.getHeight();

        if(height > (windowHeight - 100))
            return Color.GREEN;
        else if(height < (windowHeight - 100) && height > (windowHeight - 200))
            return Color.BLUE;
        else if(height < (windowHeight - 200) && height > (windowHeight - 300))
            return Color.YELLOW;
        else if(height < (windowHeight - 300) && height > (windowHeight - 400))
            return Color.ORANGE;
        else
            return Color.RED;
    }

	public class WordSprite extends Actor {

        private Body body;

		private GlyphLayout text;

        private int value;

		public WordSprite() {
            setName(getWordBasedOnDifficulty());
			text = new GlyphLayout(font, getName().toUpperCase());

            BodyDef bodyDef = new BodyDef();
            bodyDef.position.set(
                    MathUtils.random(0, Gdx.graphics.getWidth() - text.width),
                    Gdx.graphics.getHeight() + 25
            );
            bodyDef.type = BodyDef.BodyType.DynamicBody;

            body = world.createBody(bodyDef);

            PolygonShape textbox = new PolygonShape();
            textbox.setAsBox(text.width, text.height);
            body.createFixture(textbox, 0.0f);
            textbox.dispose();

            if(active == null)
                body.setLinearVelocity(0, -0.5f * PPE);
            else
                body.setLinearVelocity(0, -(active.getWordFallSpeed()) * PPE);
		}

        public WordSprite(String str, float speed) {
            setName(str);
            text = new GlyphLayout(font, getName().toUpperCase());

            BodyDef bodyDef = new BodyDef();
            bodyDef.position.set(
                    MathUtils.random(0, Gdx.graphics.getWidth() - text.width),
                    Gdx.graphics.getHeight() + 25
            );
            bodyDef.type = BodyDef.BodyType.DynamicBody;

            body = world.createBody(bodyDef);

            PolygonShape textbox = new PolygonShape();
            textbox.setAsBox(text.width, text.height);
            body.createFixture(textbox, 0.0f);
            textbox.dispose();

            body.setLinearVelocity(0, -speed * PPE);
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            font.draw(batch, text, body.getPosition().x, body.getPosition().y);

            if(body.getPosition().y < (field.getHeight() + text.height)) {
                if(getName().equalsIgnoreCase(active.getCode())) {
                    destroy();
                } else {
                    removeScore(500);
                    destroy();
                }
            }
        }

        public void destroy() {
            field.setText("");
            world.destroyBody(body);
            remove();
        }

        public double getPossibleScore() {
            return (value * multiplier) * active.getDifficulty();
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    public String getWordBasedOnDifficulty() {
        if(active != null)
            switch(active.getDifficulty()) {
                case 1:
                case 2:
                case 3:
                    return oneSyllableVerbs.random();
                case 4:
                case 5:
                case 6:
                    return chooser.get(MathUtils.random(0, 1)).random();
                case 7:
                case 8:
                    return chooser.get(MathUtils.random(0, 2)).random();
                case 9:
                case 10:
                default:
                    return chooser.get(MathUtils.random(0, 3)).random();
            }
        return oneSyllableVerbs.random();
    }

    public class Level {
        private String quickCode;
        private String code;
        private int difficulty;
        private float minSpeed;
        private float maxSpeed;
        private boolean hasBoss;
        private BigInteger passingScore;
        private float wordFallSpeed;
        private boolean levelCodeSpawned = false;

        public String getQuickCode() {
            return quickCode;
        }

        public void setQuickCode(String quickCode) {
            this.quickCode = quickCode;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code.toLowerCase();
        }

        public int getDifficulty() {
            return difficulty;
        }

        public void setDifficulty(int difficulty) {
            this.difficulty = difficulty;
        }

        public float getMinSpeed() {
            return minSpeed;
        }

        public void setMinSpeed(float minSpeed) {
            this.minSpeed = minSpeed;
        }

        public float getMaxSpeed() {
            return maxSpeed;
        }

        public void setMaxSpeed(float maxSpeed) {
            this.maxSpeed = maxSpeed;
        }

        public boolean isHasBoss() {
            return hasBoss;
        }

        public void setHasBoss(boolean hasBoss) {
            this.hasBoss = hasBoss;
        }

        public BigInteger getPassingScore() {
            return passingScore;
        }

        public void setPassingScore(int passingScore) {
            this.passingScore = BigInteger.valueOf(passingScore);
        }

        public float getWordFallSpeed() {
            return wordFallSpeed;
        }

        public void setWordFallSpeed(float wordFallSpeed) {
            this.wordFallSpeed = wordFallSpeed;
        }

        public boolean isPassable() {
            return score.intValue() > passingScore.intValue();
        }

        public void clampSpeed() {
            speed = MathUtils.clamp(speed, maxSpeed, minSpeed);
        }

        public void spawnLevelCode() {
            if(score.intValue() >= (getPassingScore().intValue() / 2))
                if(MathUtils.randomBoolean(50f) && !levelCodeSpawned) {
                    words.addActor(new WordSprite(getCode(), 1.5f));
                    levelCodeSpawned = true;
                }
        }
    }
}