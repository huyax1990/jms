package com.net023;

import javax.jms.*;
import javax.naming.InitialContext;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;

/**
 * Created by hadoop on 2015/4/27.
 */
public class Chat implements MessageListener{
    private TopicSession pubSession;
    private TopicPublisher publisher;
    private TopicConnection connection;
    private String username;

    public Chat() {
    }

    /*用于初始化chat(聊天)的构造函数*/
    public Chat(String topicFactory,String topicName,String username)
        throws Exception{
        //使用jndi.properties文件获取一个JNDI连接
        InitialContext ctx = new InitialContext();
        //查找一个jms连接工厂并创建连接
        TopicConnectionFactory conFactory = (TopicConnectionFactory) ctx.lookup(topicFactory);
        TopicConnection connection = conFactory.createTopicConnection();
        //创建两个jms会话对象(发布会话,接收会话)
        TopicSession pubSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
        TopicSession subSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
        //查找一个jms主题
        Topic chatTopic = (Topic) ctx.lookup(topicName);
        //创建一个jms发布者和订阅者.createSubscriber中附加的参数是一个消息
        //选择器(null)和noLocal标记的一个真值,它表明这个发布者生产的消息不应被它自己所消费
        TopicPublisher publisher = pubSession.createPublisher(chatTopic);
        TopicSubscriber subscriber = subSession.createSubscriber(chatTopic, null, true);
        //设置一个jms消息侦听器
        subscriber.setMessageListener(this);
        //初始化chat应用程序变量
        this.connection = connection;
        this.pubSession = pubSession;
        this.publisher = publisher;
        this.username = username;
        //启动jms链接,允许传送消息
        connection.start();
    }

    /*接收来自TopicSubscriber的消息*/
    @Override
    public void onMessage(Message message) {
        try {
            TextMessage textMessage = (TextMessage) message;
            String text = textMessage.getText();
            System.out.println(text);
        }catch (JMSException exception){
            exception.printStackTrace();;
        }
    }

    //使用发布者创建并发送消息
    protected void writeMessage(String text) throws JMSException{
        TextMessage message = pubSession.createTextMessage(text);
        message.setText(username+":"+text);
        publisher.publish(message);
    }

    //关闭jms链接
    public void close() throws JMSException{
        connection.close();
    }


    //运行聊天客户端
    public static void main(String[] args) {
        try {
            /*if(args.length!=3){
                System.out.println("Factory.Topic.or unsername missing");
            }else{
                Chat chat = new Chat(args[0], args[1], args[2]);
                //从命令行读取
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                while (true){
                    String line = reader.readLine();
                    if("exit".equalsIgnoreCase(line)){
                        chat.close();
                        System.exit(0);
                    }else{
                        chat.writeMessage(line);
                    }
                }
            }*/
            System.out.print("兄弟请输入你的名字:");
            Scanner scanner = new Scanner(System.in);
            String username = "none";
            String connectionFactoryNames = "TopicCF";
            String topic = "topic1";
            username = scanner.nextLine().trim();
            Chat chat = new Chat(connectionFactoryNames,topic,username);
            //从命令行读取
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while (true){
                String line = reader.readLine();
                if("exit".equalsIgnoreCase(line)){
                    chat.close();
                    System.exit(0);
                }else{
                    chat.writeMessage(line);
                }
            }
        }catch (Exception exp){
            exp.printStackTrace();
        }
    }

}
