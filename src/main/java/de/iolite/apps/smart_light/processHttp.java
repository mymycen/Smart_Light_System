package de.iolite.apps.smart_light;

import de.iolite.common.requesthandler.IOLITEHTTPRequest;
import de.iolite.common.requesthandler.IOLITEHTTPStaticResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Leo on 22.06.2017.
 */
public class processHttp {

    private String getCharset(final IOLITEHTTPRequest request) {
        final String charset = request.getCharset();
        return charset == null || charset.length() == 0 ? IOLITEHTTPStaticResponse.ENCODING_UTF8 : charset;
    }

    public String readPassedData(final IOLITEHTTPRequest request) throws IOException {
        final String charset = getCharset(request);
        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(request.getContent(), charset))) {
            final StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                System.out.println(line);
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        }
    }

    public String readJSONData(final IOLITEHTTPRequest request) throws IOException {
        final String charset = getCharset(request);
        String line = "{}";
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(request.getContent(), charset))) {
            if ((line = bufferedReader.readLine()) != null) {
                return line;
            }
        }
        return line;
    }
}
