
package com.zhuoan.enumtype;

public enum EnvKeyEnum {
    RUN_ENVIRONMENT("run_environment"),
    LOCAL_NAME("local_name"),
    LOCAL_PORT("local_port"),
    SERVER_IP("server_ip"),
    SERVER_PORT("server_port");

    private String key;

    private EnvKeyEnum(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
