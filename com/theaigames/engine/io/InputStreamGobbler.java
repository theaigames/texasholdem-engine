// Copyright 2015 theaigames.com (developers@theaigames.com)

//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at

//        http://www.apache.org/licenses/LICENSE-2.0

//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//	
//    For the full copyright and license information, please view the LICENSE
//    file that was distributed with this source code.

package com.theaigames.engine.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * InputStreamGobbler class
 * 
 * @author Jackie Xu <jackie@starapple.nl>, Jim van Eeden <jim@starapple.nl>
 */
public class InputStreamGobbler extends Thread {
	
	private InputStream inputStream;
	private IOPlayer player;
	private String type;
	private StringBuffer buffer;

	InputStreamGobbler(InputStream inputStream, IOPlayer player, String type) {
        this.inputStream = inputStream;
        this.player = player;
        this.type = type;
        this.buffer = new StringBuffer();
    }

    public void run() {
    	String lastLine;
    	
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(this.inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            while ((lastLine = bufferedReader.readLine()) != null) {
                if (!lastLine.contains("VM warning") && buffer.length() < 1000000) { //catches bots that return way too much (infinite loop)
                    if (this.type.equals("output"))
                	   this.player.response = lastLine;
                    buffer.append(lastLine + "\n");
                }
            }
            try {
            	bufferedReader.close();
            } catch (IOException e) {}
            
        } catch (IOException x) {
            throw new RuntimeException(x);
        }
    }
    
    public String getData() {
		return buffer.toString();
	}
}
