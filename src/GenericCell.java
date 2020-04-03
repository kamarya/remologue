/*
 *   Copyright 2017 Behrooz Kamary
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

import javafx.scene.control.TableCell;
import javafx.scene.paint.Color;
import model.SyslogItem;

public class GenericCell extends TableCell<SyslogItem, String>
{
    @Override
    protected void updateItem(String item, boolean empty)
    {
        super.updateItem(item, empty);

        if (item == null || empty)
        {
            setText(null);
            setStyle("");
        }
        else
        {
            setText(item);
            setStyle("-fx-font-family: Courier New;-fx-font-size:10;");
            SyslogItem auxItem = getTableView().getItems().get(getIndex());
            setTextFill(Color.BLACK);
        }
    }
}
