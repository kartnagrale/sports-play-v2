package com.neml.badminton.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.util.*;

@Component
public class AllowedOrigins {
    private final List<String> values;
    public AllowedOrigins(@Value("${app.cors.allowed-origins}") String configured) {
        values=Arrays.stream(configured.split(",")).map(String::trim).distinct().toList();
        if(values.isEmpty())throw new IllegalArgumentException("Explicit origins required");
        for(String value:values){
            URI uri=URI.create(value);
            if(!("http".equals(uri.getScheme())||"https".equals(uri.getScheme()))||uri.getHost()==null||value.contains("*")
                    ||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null
                    ||(uri.getPath()!=null&&!uri.getPath().isEmpty()))throw new IllegalArgumentException("Origins must be explicit http(s) origins without a path");
        }
    }
    public List<String> values(){return values;}
}
