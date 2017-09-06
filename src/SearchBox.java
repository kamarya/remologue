/*
 *   Copyright 2017 Behrooz Kamary Aliabadi
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

public class SearchBox extends TextField implements ChangeListener<String>
{
    public SearchBox()
    {
        getStyleClass().setAll("search-box");
        setPromptText("Filter");
        textProperty().addListener(this);
        setPrefHeight(20);
    }

    @Override
    protected void layoutChildren()
    {
        super.layoutChildren();
    }

    @Override
    public void changed(ObservableValue<? extends String> ov, String oldValue, String newValue)
    {
    }
}
