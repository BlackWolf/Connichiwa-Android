package de.marioschreiner.connichiwa_android;

import android.content.Context;
import android.util.Log;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by BlackWolf on 22/02/15.
 */
public class CWHTTPServer extends NanoHTTPD {

    public CWServerManagerDelegate delegate;
    private Context context;

    private Map<String, CWHTTPHandler> handler = new LinkedHashMap<String, CWHTTPHandler>();

    public CWHTTPServer(int port, Context context) {
        super(port);

        this.context = context;
    }

    public void addPathHandler(String basePath, String directoryPath) {
        //basePath must be a directory
        if (basePath.endsWith("/") == false) {
            basePath += "/";
        }
        final String aBasePath = basePath;

        //directoryPath must be a directory
        if (directoryPath.endsWith("/") == false) {
            directoryPath += "/";
        }
        final String aDirectoryPath = directoryPath;

        CWHTTPHandler handler = new CWHTTPHandler() {
            @Override
            public Response handle(String request) {
                //We don't serve directories, only files
                //Therefore, if a directory is requested, try to serve the index.html inside of it
                if (request.endsWith("/")) {
                    request += "index.html";
                }

                String assetPath = request.replaceFirst(aBasePath, aDirectoryPath);

                return assetResponse(context, assetPath);
            }
        };

        this.addHandler(aBasePath, handler);
    }

    public void addFileHandler(final String basePath, final String filePath) {
        CWHTTPHandler handler = new CWHTTPHandler() {
            @Override
            public Response handle(String request) {
                //This handler serves only a single file, therefore the basePath must be exactly
                //the specified basePath
                if (request.equals(basePath)) {
                    return assetResponse(context, filePath);
                }

                return null; //we don't handle this request
            }
        };

        this.addHandler(basePath, handler);
    }

    public void addHTMLHandler(final String basePath, final String html) {
        CWHTTPHandler handler = new CWHTTPHandler() {
            @Override
            public Response handle(String request) {
                //This handler serves a static file. Serve only the specified base path
                if (request.equals(basePath)) {
                    return htmlResponse(html);
                }

                return null; //we don't handle this request
            }
        };

        this.addHandler(basePath, handler);
    }

    public void addTextHandler(final String basePath, final String text) {
        CWHTTPHandler handler = new CWHTTPHandler() {
            @Override
            public Response handle(String request) {
                //This handler serves a static file. Serve only the specified base path
                if (request.equals(basePath)) {
                    return textResponse(text);
                }

                return null; //we don't handle this request
            }
        };

        this.addHandler(basePath, handler);
    }

    public void addHandler(String path, CWHTTPHandler handler) {
        this.handler.put(path, handler);
    }

    @Override
    public Response serve(IHTTPSession session) {
        Map<String, String> header = session.getHeaders();
        Map<String, String> parms = session.getParms();
        String uri = session.getUri();

        Log.d("HTTP", "Request to URI " + uri);

        //Traverse handlers in reversed order
        //Although I think that a FIFO order makes more sense, we do it in LIFO order
        //to make it compliant with the GDCWebserver used in the iOS version
        LinkedList<String> keyList = new LinkedList<>(this.handler.keySet());
        Iterator<String> keyIterator = keyList.descendingIterator();
        while (keyIterator.hasNext()) {
            String key = keyIterator.next();

            if (uri.startsWith(key)) {
                CWHTTPHandler handler = this.handler.get(key);
                Response result = handler.handle(uri);

                //If the handler returns a non-null result, that's our response and we are done
                if (result != null) {
                    return result;
                }
            }
        }

        return CWHTTPHandler.notFoundResponse(); //no handler handled this uri
    }


}
