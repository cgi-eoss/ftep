package com.cgi.eoss.ftep.clouds.ipt.persistence;

import com.cgi.eoss.ftep.clouds.ipt.IptPersistenceConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {IptPersistenceConfiguration.class})
@TestPropertySource("classpath:test-ipt-persistence.properties")

public class KeypairRepositoryTest {

    @Autowired
    KeypairRepository keypairRepository;

    private String samplePrivateKey = "MIIEqQIBAAKCAQEAtqMHQbAzgjFN1zv8lWnVfQkZ0Wj8EC2BnLiLuE9m2iYK3Euz\n" +
            "y4JXD6plhc91lBOX89nwAxFu3edOBMPlEqkjuEUJKauL6zyB/fAToHjmiH0jy6Z4\n" +
            "ROn/lT+EGy50rHq8UZqEOW8PcHJwT70/jKYWfKQgQzqfkWuxqujcOYa+3iB4i9sJ\n" +
            "a5jqQQCamHE+uVm6I5L/7iHZd/OpBJAPG/qLwf+jaqXNUKYgpVC23z5BHh/VSDl4\n" +
            "LSX9N9VH1s1EouLmc4r2C1eXEARXl/Bigf2vN4J1diDjjH6M36uaDGadjnpRAv0/\n" +
            "PAgtQOYgpQZbORw4WaO5kwWei3J+eCzStalldwIDAQABAoIBACl1dqc9tWOfh5kN\n" +
            "X/gW70ST9U1pNJcDjYGjDuyG8cUhj6OUd7sB2nuO5b7rajhz/jF3zpkTswQihr5k\n" +
            "LbL/vEEPQtY3v+plcOjRP9NUvphLN/35yyFxsPgTVAzLjea9cdRgbBWRlYKkJ8Si\n" +
            "JDPsW/xtUXmRbDmZpdozRBK2+Mr5V7ApUNqP6n/8/o+D5AumGVMWXO1CDpZzmxV0\n" +
            "xjJJsrAV9maFcK4k2HgAVWXCYiSUD8tdWOkFcU62+mATlaA/p/kPzIbYKtYJYnn0\n" +
            "B+ljGHre0SWQek+TCZHzzytVh4m/6zMVo7e7rI4CKXwhb5Lw4JIrD7lavA+sab6a\n" +
            "rNpXstkCggCBAMxUZKjtH9SmV1IgUCDSCVTx0/AToiVwxn7bOOC5L2nGY6bEEcMH\n" +
            "wDQcp2RzSQMB2Y7+MdQLe1Dn+ws6j4+EYH+orZwLEV4HQveB/u0BSqkvcHdd7g+T\n" +
            "7SU5gtfFLWLgQiKlbC5J+IzUZZGZXhbmH7P7C8XEi2htlIicxaDv7jcjAoIAgQDk\n" +
            "0lCoQKZA88Es2/YJPrW3ENDn+PCN8ECZqrGdL4iKAe47UCV6Dm8uB3xv7nLB/dAw\n" +
            "PRq4RoUItwZ+XDqUr3Z5BTPyueZYujceiRbAJJGbvV3CETf5rBudfgqXchN6Qin8\n" +
            "TttWnQfcamP32mAnejrEFs8fUV53Ht/G9y+51XvnnQKCAIEAjlUrWVD0n1jP8vF3\n" +
            "X6VvwrBCBjZm+NW6L5vd3wygrnn36K8kAEth4+sh36M+9M1RE36WjzMAT7eI9KVy\n" +
            "Y3K/Fa4WYWCEVI+SYn83aTlBHOFi4oOX1VoiE5AgSNWdbAbzitbYqNX2QYOizO2c\n" +
            "0S60Xtc9uCHVSoUqto8eS6FWuykCggCBAMUq8DM6mcdIAoF7+7IVphDbXZc1K1qy\n" +
            "1YdTzP4LklSQu03CjrhwrZ509uWgCqRb50NZs6drpIBQ7Th+kj/CPYykThHt261C\n" +
            "r/IQ4FiqaglPj/WETr5Fbfo0PD4pHt1vG6x76oXkg3yK9B8CvW3m5bJHkSuEOqjc\n" +
            "IqjYH/i6Gq0FAoIAgHSQEZ2MJs5viZwWPd8JzMxoNq68eu9q+9VbyIA+ky3MQoLQ\n" +
            "MLZY///4/uI48yDCs5rWj7PPYIl1voFzxOEAxUeYhVkrYL8IjrPJjiR0lsLVdm0q\n" +
            "dpqvtsZVNFFvuQU17nO3E1xroNcIbAY8romJA3ZItZtZa4cnaKPEcFMq9VAV\n" +
            "-----END RSA PRIVATE KEY-----";

    private String samplePublicKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC2owdBsDOCMU3XO/yVadV9CRnRaPwQLYGcuIu4T2baJgrcS7PLglcPqmWFz3WUE5fz2fADEW7d504Ew+USqSO4RQkpq4vrPIH98BOgeOaIfSPLpnhE6f+VP4QbLnSserxRmoQ5bw9wcnBPvT+MphZ8pCBDOp+Ra7Gq6Nw5hr7eIHiL2wlrmOpBAJqYcT65Wbojkv/uIdl386kEkA8b+ovB/6Nqpc1QpiClULbfPkEeH9VIOXgtJf031UfWzUSi4uZzivYLV5cQBFeX8GKB/a83gnV2IOOMfozfq5oMZp2OelEC/T88CC1A5iClBls5HDhZo7mTBZ6Lcn54LNK1qWV3 Generated-by-Nova";

    @Test
    public void testKeypairCreation() {
        Keypair kp = new Keypair("servId", samplePrivateKey, samplePublicKey);
        keypairRepository.save(kp);
    }

    @Test
    public void testKeypairRetrieval() {
        Keypair kp = new Keypair("servId", samplePrivateKey, samplePublicKey);
        keypairRepository.save(kp);
        Keypair kpRetrieved = keypairRepository.findOne("servId");
        assertNotNull(kpRetrieved);
        assertEquals(kp.getPrivateKey(), samplePrivateKey);
    }

    @Test
    public void testKeypairDeletion() {
        Keypair kp = new Keypair("servId", "privKey", "pubKey");
        kp = keypairRepository.save(kp);
        keypairRepository.delete(kp);
        Keypair kpRetrieved = keypairRepository.findOne("servId");
        assertNull(kpRetrieved);
    }

}
