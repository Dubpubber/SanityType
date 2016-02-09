package com.loneboat.sanitytype;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
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
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;

import java.io.IOException;
import java.math.BigInteger;

public class SanityType extends ApplicationAdapter {

    private static final float PPE = 100;

	SpriteBatch batch;
	BitmapFont font;

	private Array<String> fourLetter_verbs;

    private int level_index;
    private Level active;
    private Array<Level> levels;

    private Stage words;
    private Stage input;

    private ShapeRenderer renderer;

    private World world;

    private WordSprite focus;
    private TextField field;

    private float timer = 0;
    private float speed = 2;

    private BigInteger score = BigInteger.valueOf(0);
    private double multiplier = 1.0;

    @Override
	public void create () {
		Gdx.app.setLogLevel(Application.LOG_DEBUG);
		font = new BitmapFont();
        batch = new SpriteBatch();

        words = new Stage();
        input = new Stage();

        renderer = new ShapeRenderer();

        world = new World(new Vector2(0, 0), true);

		fourLetter_verbs = new Array<String>();
		String[] FL_verbs = Gdx.files.internal("4L_verbs.txt").readString().split(",");
		fourLetter_verbs.addAll(FL_verbs);

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
                log(child.getName());
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

        setLevel(0);
        Gdx.input.setInputProcessor(input);

	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if(active == null) {
            Gdx.graphics.setTitle(Gdx.graphics.getFramesPerSecond() + " " + Gdx.input.getX() + " , " + Gdx.input.getY() + " (!Loading Level!)");
            return;
        }
        Gdx.graphics.setTitle(Gdx.graphics.getFramesPerSecond() + " " + Gdx.input.getX() + " , " + Gdx.input.getY());

        // Add words based on timer.
        timer += Gdx.graphics.getDeltaTime();
        if(timer >= speed) {
            addWordSprite();
            timer = 0;
        }

        if(active.isPassable())
            advanceLevel();

        words.act();
        words.draw();

        input.act();
        input.draw();

        batch.begin();
        font.draw(batch, "Score: " + score.intValue(), 5, 585);
        font.draw(batch, "Multiplier: " + multiplier, 5, 565);
        font.draw(batch, "Speed: " + speed, 5, 545);
        font.draw(batch, "Level: " + active.getQuickCode(), 5, 525);
        if(words.getActors().size > 0)
            focus = (WordSprite) words.getActors().first();
        else
            focus = addWordSprite();
        batch.end();

        if(focus != null) {
            renderer.begin(ShapeRenderer.ShapeType.Line);
            renderer.setColor(Color.ORANGE);
            renderer.line(
                    0,
                    focus.body.getPosition().y - (focus.text.height / 2),
                    focus.body.getPosition().x - 5,
                    focus.body.getPosition().y - (focus.text.height / 2)
            );
            renderer.line(
                    focus.body.getPosition().x + (focus.text.width + 5),
                    focus.body.getPosition().y - (focus.text.height / 2),
                    Gdx.graphics.getWidth(),
                    focus.body.getPosition().y - (focus.text.height / 2)
            );
            renderer.end();
        }

        if(Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY)) {
            int length = field.getText().length();
            if(length > 0 && length <= focus.getName().length()) {
                correctChar(length - 1);
                if(checkFocus(length - 1)) {
                    focus.destroy();
                    addScore(250);
                    multiplier += 0.5;
                }
            }
        }

        if(Gdx.input.isKeyPressed(Input.Keys.ESCAPE))
            Gdx.app.exit();

        world.step(1/60f, 6, 2);

	}

    public void correctChar(int index) {
        char[] f_chr = focus.getName().toCharArray();
        char[] field_chr = field.getText().toCharArray();
        if(f_chr[index] != field_chr[index]) {
            removeScore(100);
            multiplier = 1;
            incSpeed(0.05f);
        }
    }

    public boolean checkFocus(int index) {
        char[] f_chr = focus.getName().toCharArray();
        char[] field_chr = field.getText().toCharArray();
        return (f_chr[index] == field_chr[index]) && (f_chr.length == field_chr.length);
    }

    public WordSprite addWordSprite() {
        WordSprite ws = new WordSprite();
        words.addActor(ws);
        return ws;
    }

    public void addScore(int value) {
        double result = (value * multiplier) * active.getDifficulty();
        score = score.add(BigInteger.valueOf((long) result));
    }

    public void removeScore(int value) {
        score = score.subtract(BigInteger.valueOf(value));
    }

    public void incSpeed(float speed) {
        this.speed -= speed;
    }

    public Level getLevel(String id) {
        for(Level level : levels)
            if(level.getCode().equals(id))
                return level;
        return null;
    }

    public void setLevel(int index) {
        if(index < levels.size)
            this.active = levels.get(index);
    }

    public void advanceLevel() {
        if(level_index < levels.size)
            this.active = levels.get(level_index + 1);
    }

	public void log(String message) {
		Gdx.app.debug("DebugOut", message);
	}

	public class WordSprite extends Actor {

        private Body body;

		private GlyphLayout text;

		public WordSprite() {
            setName(fourLetter_verbs.random());
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

        @Override
        public void draw(Batch batch, float parentAlpha) {
            font.draw(batch, text, body.getPosition().x, body.getPosition().y);

            if(body.getPosition().y < 45) {
                removeScore(500);
                destroy();
            }
        }

        public void destroy() {
            field.setText("");
            world.destroyBody(body);
            remove();
        }
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
            this.code = code;
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
    }

}