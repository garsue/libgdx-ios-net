package fm.garsue.ios.net;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class IOSNetGame extends ApplicationAdapter {

    private static final String TAG = "IOS-NET";

    SpriteBatch batch;
    BitmapFont font;
    final StringBuilder text = new StringBuilder("Initial text");

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();

        {//failure test
            final Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
            final String url = "http://example.invalid";
            request.setUrl(url);
            sendRequest(request, url);
        }
        {//cancel test
            final Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
            final String url = "http://example.com";
            request.setUrl(url);
            sendRequest(request, url);
            Gdx.net.cancelHttpRequest(request);
        }
        {//success test
            final Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
            final String url = "http://example.com";
            request.setUrl(url);
            sendRequest(request, url);
        }
        {//https test
            final Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.GET);
            final String url = "https://example.com";
            request.setUrl(url);
            sendRequest(request, url);
        }
    }

    private void sendRequest(final Net.HttpRequest request, final String url) {
        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {

            @Override
            public void handleHttpResponse(Net.HttpResponse response) {
                Gdx.app.debug(TAG, "success " + url);
                text.delete(0, text.length()).append(response.getResultAsString());
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.debug(TAG, "failed " + url, t);
            }

            @Override
            public void cancelled() {
                Gdx.app.debug(TAG, "cancelled " + url);
            }
        });
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        batch.begin();
        font.draw(batch, text, 0, 20);
        batch.end();
    }

    @Override
    public void dispose() {
        font.dispose();
        batch.dispose();
        super.dispose();
    }

}
