package org.casbin.test;

import org.casbin.adapter.RedisAdapter;
import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.main.SyncedEnforcer;
import org.casbin.watcher.RedisWatcher;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RedisWatcherZwsTest {
    private RedisWatcher redisWatcher, redisConfigWatcher;
    private final String expect = "update msg";
    private final String expectConfig = "update msg for config";

    private String host = "localhost";
    private int port = 6379;
    private RedisAdapter redisAdapter;
    private RedisAdapter redisAdapter2;

    @Test
    public void testConfigUpdate() throws InterruptedException {


        String redisTopic = "jcasbin-topic";
        RedisWatcher redisWatcher = new RedisWatcher("127.0.0.1", 6379, redisTopic);
        // Support for connecting to redis with timeout and password
        // RedisWatcher redisWatcher = new RedisWatcher("127.0.0.1",6379, redisTopic, 2000, "foobared");

        Enforcer enforcer = new SyncedEnforcer("examples/rbac_model.conf", "examples/rbac_policy.csv");
        enforcer.setWatcher(redisWatcher);


        redisAdapter = new RedisAdapter(host, port);

        redisAdapter2 = new RedisAdapter(host, port);

        // This is a trick to save the current policy to the DB.
        // We can't call e.SavePolicy() because the adapter in the enforcer is still the file adapter.
        // The current policy means the policy in the Casbin enforcer (aka in memory).
        redisAdapter.savePolicy(enforcer.getModel());

        // Clear the current policy.
        enforcer.clearPolicy();


        testGetPolicy(enforcer, new ArrayList<>());

        enforcer = new SyncedEnforcer("examples/rbac_model.conf", redisAdapter);
        enforcer.clearPolicy();
        enforcer.setWatcher(redisWatcher);

        // Load the policy from DB.
        redisAdapter.loadPolicy(enforcer.getModel());

        testGetPolicy(enforcer, Arrays.asList(Arrays.asList("alice", "data1", "read"),
                Arrays.asList("bob", "data2", "write"),
                Arrays.asList("data2_admin", "data2", "read"),
                Arrays.asList("data2_admin", "data2", "write")));


        String redisTopic1 = "jcasbin-topic";
        RedisWatcher redisWatcher1 = new RedisWatcher("127.0.0.1", 6379, redisTopic);

        Enforcer enforcer1 = new SyncedEnforcer("examples/rbac_model.conf", redisAdapter2);
        enforcer1.setWatcher(redisWatcher1);

        enforcer.addPolicy(Arrays.asList("9999", "data1", "write"));

//        redisAdapter.savePolicy(enforcer.getModel());

        List<List<String>> list00 = enforcer.getPolicy();
        List<List<String>> list11 = enforcer1.getPolicy();

        System.out.println(list11);

        list11 = enforcer1.getPolicy();

        enforcer.addPolicy(Arrays.asList("99888", "data1", "write"));
        System.out.println(list00);
        System.out.println(list11);


    }

    private void testGetPolicy(Enforcer e, List<List<String>> res) {
        List<List<String>> policies = e.getPolicy();
        Assert.assertEquals(res, policies);
    }

}
