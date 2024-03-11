/*
 * Copyright 2021-2022 floragunn GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.floragunn.searchguard.sgctl.client.api;

import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.util.ArrayList;
import java.util.Date;


public class AuthTokenDTO {

    public AuthTokenDTO() {
    }

    public String getUser_name() {
        return user_name;
    }

    public String getToken_name() {
        return token_name;
    }
    public Requested getRequested() {
        return requested;
    }
    public String getCreated_at() {
        Date date=new Date(created_at * 1000);
        return  date.toString();
    }

    public String getExpires_at() {
        Date date=new Date(expires_at * 1000);
        return date.toString();
    }

    public int getRevoked_at() {
        return revoked_at;
    }
    private String user_name;
    private String token_name;
    private Requested requested;
    private Long created_at;
    private Long expires_at;
    private int revoked_at;

}

 class IndexPermission{
    private ArrayList<String> index_patterns;
    private ArrayList<String> allowed_actions;

     public ArrayList<String> getIndex_patterns() {
         return index_patterns;
     }

     public ArrayList<String> getAllowed_actions() {
         return allowed_actions;
     }
     @Override
     public String toString() {
         return "{" +
                 "index_patterns=" + index_patterns +
                 "\n  allowed_actions=" + allowed_actions +
                 '}';

     }
 }

 class Requested{
     private ArrayList<String> cluster_permissions;
     private ArrayList<IndexPermission> index_permissions;

     public ArrayList<IndexPermission> getCluster_index_permissions() {
         return index_permissions;
     }

     public ArrayList<String> getCluster_permissions() {

         return cluster_permissions;
     }
//     public String toYaml(){
//         ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
//         String yml;
//         try {
//             yml = mapper.writeValueAsString(this);
//         } catch (JsonProcessingException e) {
//             throw new RuntimeException(e);
//         }
//         System.out.println(yml);
//         return yml;
//     }
     @Override
     public String toString() {
         return "{" +
                 "cluster_permissions=" + cluster_permissions +
                 ", index_permissions=" + index_permissions +
                 '}';

     }
 }




