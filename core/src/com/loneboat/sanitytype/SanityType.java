package com.loneboat.sanitytype;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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

        spawnerTask = new Timer().scheduleTask(new Timer.Task() {
            @Override
            public void run() {
                // add one, make one fall.
                words.addActor(new WordSprite(fourLetter_verbs.random()));
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
        Gdx.graphics.setTitle(Gdx.graphics.getFramesPerSecond() + "");

        words.act();
        words.draw();

        input.act();
        input.draw();

        batch.begin();
        if(words.getActors().size > 0)
            focus = (WordSprite) words.getActors().first();
        // font.draw(batch, focus.text, (Gdx.graphics.getWidth() - focus.text.width) / 2, 75);
        batch.end();

        if(focus != null) {
            renderer.begin(ShapeRenderer.ShapeType.Line);
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

        if(Gdx.input.isKeyPressed(Input.Keys.ENTER) && field.getText().length() > 0)
            query();

        world.step(1/60f, 6, 2);

	}

    public void query() {
        if(focus.getName().equalsIgnoreCase(field.getText().toLowerCase()))
            focus.remove();
        field.setText("");
    }

    public void speedUp(float speed) {
        this.speed -= speed;
        spawnerTask.cancel();

        spawnerTask = new Timer().scheduleTask(new Timer.Task() {
            @Override
            public void run() {
                // add one, make one fall.
                words.addActor(new WordSprite(fourLetter_verbs.random()));
            }
        }, 0, speed);
    }

	public void log(String message) {
		Gdx.app.debug("DebugOut", message);
	}

	public class WordSprite extends Actor {

        private Body body;

		private GlyphLayout text;

		public WordSprite(String str) {
			text = new GlyphLayout(font, str.toUpperCase());
            setName(str);

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

            if(body.getPosition().y < 50) {
                remove();
            }

        }
	}
}