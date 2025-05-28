package com.tlmqtt.auth;

import com.tlmqtt.auth.fix.FixTlAuthentication;
import com.tlmqtt.auth.http.HttpEntityInfo;
import com.tlmqtt.auth.http.HttpTlAuthentication;
import com.tlmqtt.auth.sql.SqlEntityInfo;
import com.tlmqtt.auth.sql.SqlTlAuthentication;
import com.tlmqtt.common.model.entity.TlUser;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @Author: hszhou
 * @Date: 2025/1/17 17:16
 * @Description: 认证链 只要有任何一个认证通过即可
 */
@Slf4j
public class AuthenticationManager extends AbstractTlAuthentication {



    private final NoneAuthenticationService headAuthentication;

    public AuthenticationManager(boolean auth){

        headAuthentication = new NoneAuthenticationService(()->auth);
        FixTlAuthentication fixTlAuthentication = new FixTlAuthentication(new ArrayList<>());
        HttpTlAuthentication httpTlAuthentication = new HttpTlAuthentication(new ArrayList<>());
        SqlTlAuthentication sqlTlAuthentication = new SqlTlAuthentication(new ArrayList<>());
        headAuthentication.setNextAuthentication(fixTlAuthentication);
        fixTlAuthentication.setNextAuthentication(httpTlAuthentication);
        httpTlAuthentication.setNextAuthentication(sqlTlAuthentication);
    }


    public void setAuth(Boolean auth){
        headAuthentication.setEnabled(auth);
    }


    public void addHttpEntity(Supplier<List<HttpEntityInfo>> supplier){
        AbstractTlAuthentication authentication = headAuthentication;
        while (authentication.nextAuthentication!=null ){
            authentication = authentication.nextAuthentication;
            if(authentication instanceof HttpTlAuthentication){
                 ((HttpTlAuthentication) authentication).setHttpEntityInfos(supplier.get());
                break;
            }
        }
    }


    public void addSqlEntity(Supplier<List<SqlEntityInfo>> supplier){
        AbstractTlAuthentication authentication = headAuthentication;
        while (authentication.nextAuthentication!=null ){
            authentication = authentication.nextAuthentication;
            if(authentication instanceof SqlTlAuthentication){
                ((SqlTlAuthentication) authentication).setSqlEntityInfos(supplier);
                break;
            }
        }
    }

    public void addFixUsers(List<TlUser> users){
        AbstractTlAuthentication authentication = headAuthentication;
        while (authentication.nextAuthentication!=null ){
            authentication = authentication.nextAuthentication;
            if(authentication instanceof FixTlAuthentication){
                ((FixTlAuthentication) authentication).setUsers(users);
                break;
            }
        }
    }


    /**
     * 添加过滤器链
     * @author hszhou
     * @datetime: 2025-05-15 18:27:50
     * @param tlAuthentication 认证过滤器
     **/
    public void addAuthentication(AbstractTlAuthentication tlAuthentication){
        AbstractTlAuthentication authentication = headAuthentication;
        while (authentication.nextAuthentication!=null){
            authentication = authentication.nextAuthentication;
        }
        authentication.setNextAuthentication(tlAuthentication);
    }
    /**
     *
     * @author 认证
     * @datetime: 2025-05-10 16:16:44
     * @param username 用户名
     * @param password 密码
     * @return boolean
     **/
    @Override
    public boolean authenticate(String username, String password) {
        return headAuthentication.execute(username,password);
    }

    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public void add(Object object) {

        AbstractTlAuthentication authentication = headAuthentication;

        while (authentication.nextAuthentication!=null){
            authentication.add(object);
            authentication = authentication.nextAuthentication;
        }
        //最后一个也要添加
        authentication.add(object);
    }


}
