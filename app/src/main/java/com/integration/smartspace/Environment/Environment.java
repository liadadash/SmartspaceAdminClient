package com.integration.smartspace.Environment;

/**
 * Created by liadkh on 5/22/19.
 */
public interface Environment {

    String BASE_URL = "BaseURL";
    String LOGIN = "/smartspace/users/login";
    String ACTIONS = "/smartspace/admin/actions";
    String USERS = "/smartspace/admin/users";
    String ELEMENTS = "/smartspace/admin/elements";

    enum FuncTypeEnum {
        IMPORT_DATA, EXPORT_DATA
    }

    enum DataTypeEnum {
        USERS, ELEMENTS, ACTIONS
    }

}
