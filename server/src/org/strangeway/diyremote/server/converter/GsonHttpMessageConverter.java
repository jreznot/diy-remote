/*
 * Copyright (c) 2013, DIY-Remote Contributors
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * Neither the name of the StrangeWay.org nor the names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.strangeway.diyremote.server.converter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.Assert;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

/**
 * @author Yuriy Artamonov
 */
public class GsonHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    private Gson gson;

    private Type type = null;

    private boolean prefixJson = false;

    public GsonHttpMessageConverter() {
        this(new Gson());
    }

    public GsonHttpMessageConverter(boolean serializeNulls) {
        this(serializeNulls ? new GsonBuilder().serializeNulls().create() : new Gson());
    }

    public GsonHttpMessageConverter(Gson gson) {
        super(new MediaType("application", "json", DEFAULT_CHARSET));
        setGson(gson);
    }

    public void setGson(Gson gson) {
        Assert.notNull(gson, "'gson' must not be null");
        this.gson = gson;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public void setPrefixJson(boolean prefixJson) {
        this.prefixJson = prefixJson;
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return canRead(mediaType);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return canWrite(mediaType);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {

        Reader json = new InputStreamReader(inputMessage.getBody(), getCharset(inputMessage.getHeaders()));

        try {
            Type typeOfT = getType();
            if (typeOfT != null) {
                return this.gson.fromJson(json, typeOfT);
            } else {
                return this.gson.fromJson(json, clazz);
            }
        } catch (JsonSyntaxException ex) {
            throw new HttpMessageNotReadableException("Could not read JSON: " + ex.getMessage(), ex);
        } catch (JsonIOException ex) {
            throw new HttpMessageNotReadableException("Could not read JSON: " + ex.getMessage(), ex);
        } catch (JsonParseException ex) {
            throw new HttpMessageNotReadableException("Could not read JSON: " + ex.getMessage(), ex);
        }
    }

    @Override
    protected void writeInternal(Object o, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        OutputStreamWriter writer = new OutputStreamWriter(outputMessage.getBody(),
                getCharset(outputMessage.getHeaders()));

        try {
            if (this.prefixJson) {
                writer.append("{} && ");
            }
            Type typeOfSrc = getType();
            if (typeOfSrc != null) {
                this.gson.toJson(o, typeOfSrc, writer);
            } else {
                this.gson.toJson(o, writer);
            }
            writer.close();
        } catch (JsonIOException ex) {
            throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getMessage(), ex);
        }
    }

    private Charset getCharset(HttpHeaders headers) {
        if (headers != null && headers.getContentType() != null
                && headers.getContentType().getCharSet() != null) {
            return headers.getContentType().getCharSet();
        }
        return DEFAULT_CHARSET;
    }
}