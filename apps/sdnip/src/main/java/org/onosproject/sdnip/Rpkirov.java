/*
 * Copyright 2015-present Open Networking Foundation
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
 */

package org.onosproject.sdnip;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

/**
 * Validation of the route prefix and ASN with RPKI Validator
 * using HTTP GET and process the response in JSON format.
 */
public final class Rpkirov {

    private Rpkirov() {
    }

    /**
     * Validate route prefix and ASN with RPKI Validator socket address.
     * @param validatorip RPKI Validator socket address(IP:PORT NUMBER)
     * @param asnumber BGP route source origin ASN
     * @param prefix BGP route prefix
     * @return Return validity status for the combination of ASN and PREFIX : valid,invalid,not-found
     */
    public static String validate(String validatorip, String asnumber, String prefix) {
        try {
            //"12735" , "31.223.93.0/24"          --valid
            //"12345" , "192.168.0.0/24"          --not-found
            //"12735" , "31.223.93.0/25"           --invalid
            return callMe(validatorip, asnumber, prefix);
        } catch (Exception e) {
        //    System.out.println("Error when connecting to RPKI VALIDATOR");
            //e.printStackTrace();
            return null;
        }
    }

    private static String callMe(String validatorip, String asnumber, String prefix) throws Exception {

        String url = "http://" + validatorip + "/validity?asn=" + asnumber + "&prefix=" + prefix;

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        // optional default is GET
        con.setRequestMethod("GET");
        //add request header
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        int responseCode = con.getResponseCode();
      //  System.out.println("\nSending 'GET' request to URL : " + url);
     //   System.out.println("Response Code : " + responseCode);
        if (responseCode != 200) {
     //       System.out.println("Error when contacting RPKI VALIDATOR " + responseCode);
            return null;
        }

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        //Read JSON response and print
        JSONObject myResponse = new JSONObject(response.toString());
        JSONObject oTime = myResponse.getJSONObject("validated_route");
        JSONObject oTime2 = oTime.getJSONObject("validity");
        return oTime2.getString("state");
    }
}


