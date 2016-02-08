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
import com.badlogic.gdx.utils.Timer;

import java.math.BigInteger;

public class SanityType extends ApplicationAdapter {

    private static final float PPE = 100;

	SpriteBatch batch;
	BitmapFont font;

	private Array<String> fourLetter_verbs;

    private Stage words;
    private Stage input;

    private ShapeRenderer renderer;

    private World world;

    private WordSprite focus;
    private TextField field;

    private Timer.Task spawnerTask;
    private float speed = 2;

    private BigInteger score = BigInteger.valueOf(0);
    private double multipler = 1.0;

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

        spawnerTask = Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                words.addActor(new WordSprite());
            }
        }, 0, speed);

        field = new TextField("", new Skin(Gdx.files.internal("ui/uiskin.json")));
        field.setPosition(0, 0);
        field.setSize(Gdx.graphics.getWidth(), 50);
        field.setAlignment(Align.center);
        input.addActor(field);

        Gdx.input.setInputProcessor(input);

	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.graphics.setTitle(Gdx.graphics.getFramesPerSecond() + " " + Gdx.input.getX() + " , " + Gdx.input.getY());

        words.act();
        words.draw();

        input.act();
        input.draw();

        batch.begin();
        font.draw(batch, "Score: " + score.intValue(), 5, 585);
        font.draw(batch, "Multiplier: " + multipler, 5, 565);
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
                    addScore(1000);
                    multipler += 0.5;
                }
            }
        }

        world.step(1/60f, 6, 2);

	}

    public void correctChar(int index) {
        char[] f_chr = focus.getName().toCharArray();
        char[] field_chr = field.getText().toCharArray();
        if(f_chr[index] != field_chr[index]) {
            removeScore(100);
            multipler = 1;
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
        double result = (value * multipler);
        score = score.add(BigInteger.valueOf((long) result));
    }

    public void removeScore(int value) {
        score = score.subtract(BigInteger.valueOf(value));
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

            body.setLinearVelocity(0, -0.5f * PPE);
		}

        @Override
        public void draw(Batch batch, float parentAlpha) {
            font.draw(batch, text, body.getPosition().x, body.getPosition().y);

            if(body.getPosition().y < 45) {
                destroy();
            }
        }

        public void destroy() {
            field.setText("");
            world.destroyBody(body);
            remove();
        }
    }
}