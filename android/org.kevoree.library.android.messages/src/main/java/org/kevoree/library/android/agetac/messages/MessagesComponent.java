package org.kevoree.library.android.agetac.messages;

import android.view.LayoutInflater;
import android.content.Context;
import android.view.View;
import org.kevoree.android.framework.helper.UIServiceHandler;
import org.kevoree.android.framework.service.KevoreeAndroidService;
import org.kevoree.annotation.*;


/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 15/05/12
 * Time: 14:59
 */



@ComponentType
@Library(name = "Android")
public class MessagesComponent {
    private KevoreeAndroidService uiService = null;

    @Start
    public void start() {
        this.uiService = UIServiceHandler.getUIService();
        initUI();
    }

    private void initUI() {
        LayoutInflater inflater =
                (LayoutInflater) uiService.getRootActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.messages, null);
        uiService.addToGroup("Test", view);
    }

    @Stop
    public void stop() {

    }

    @Update
    public void update() {

    }
}
