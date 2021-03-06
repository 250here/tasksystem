package com.supervise.tasksystem.service;

import com.supervise.tasksystem.dao.*;
import com.supervise.tasksystem.model.*;
import com.supervise.tasksystem.util.VirtualTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class MarketTaskService {
    @Autowired
    MarketDao marketDao;
    @Autowired
    MarketTaskDao marketTaskDao;
    @Autowired
    MarketTaskGroupDao marketTaskGroupDao;
    @Autowired
    MarketTaskItemDao marketTaskItemDao;
    @Autowired
    ProductTypeDao productTypeDao;


    public MarketTaskItem addMarketTaskItem(int marketTaskId, int productTypeId){        //添加检测项
        MarketTask marketTask = marketTaskDao.findById(marketTaskId).get();
        ProductType productType = productTypeDao.findById(productTypeId).get();

        MarketTaskItem marketTaskItem = new MarketTaskItem();
        marketTaskItem.setFinished(false);
        marketTaskItem.setMarketTask(marketTask);
        marketTaskItem.setProductType(productType);
        marketTaskItemDao.save(marketTaskItem);

        return marketTaskItem;
    }

    public List<MarketTaskItem> getAllMarketTaskItemsOfMarket(int marketId){          //查找某市场下所有任务
        List<MarketTaskGroup> marketTaskGroupList = marketTaskGroupDao.findAll();
        List<MarketTaskItem> marketTaskItems = new ArrayList<>();
        for (MarketTaskGroup group : marketTaskGroupList){
            List<MarketTask> marketTaskList = group.getMarketTasks();
            for (MarketTask marketTask: marketTaskList){
                for (MarketTaskItem marketTaskItem : marketTask.getMarketTaskItems()){
                    if (marketTaskItem.getMarketTask().getMarket().getMarketId() == marketId){
                        marketTaskItems.add(marketTaskItem);
                    }
                }
            }
        }
        if(marketTaskItems.size() == 0){
            return null;
        }
        return marketTaskItems;
    }

    public List<MarketTaskItem> getUnfinishedMarketTaskItemsOfMarket(int marketId) {          //查找某市场未完成的类别
        List<MarketTaskItem> allMarketTaskItemList = getAllMarketTaskItemsOfMarket(marketId);
        List<MarketTaskItem> marketTaskItemList = new ArrayList<>();
        for (MarketTaskItem marketTaskItem :allMarketTaskItemList){
            if (marketTaskItem.isFinished() == false){
                marketTaskItemList.add(marketTaskItem);
            }
        }
        if(marketTaskItemList.size() == 0){
            return null;
        }
        return marketTaskItemList;
    }

    public List<MarketTaskItem> getUnfinishedMarketTaskItems(int marketTaskId){                 //查找某市场任务下未完成的类别
        Optional<MarketTask> marketTaskOptional = marketTaskDao.findById(marketTaskId);
        MarketTask marketTask = marketTaskOptional.isPresent()?marketTaskOptional.get() : null;
        if(marketTask == null){
            System.out.println("查询数据为空");
            return null;
        }
        return marketTaskItemDao.findByMarketTaskAndIsFinishedFalse(marketTask);
    }

    public int getUnqualifiedNumberInTask(int marketTaskId){                  //查看任务下不合格数
        MarketTask marketTask = marketTaskDao.findById(marketTaskId).get();
        List<MarketTaskItem> marketTaskItems = marketTask.getMarketTaskItems();
        int num = 0;
        for(MarketTaskItem item: marketTaskItems){
            num += item.getUnqualifiedNumber();
        }
        return num;
    }

    public String gradeOfMarket(int marketId){                         //查看某市场得分情况
        List<MarketTaskItem> marketTaskItems = getAllMarketTaskItemsOfMarket(marketId);
        int grade = 0;
        String record = "";
        if(marketTaskItems.size() == 0){
            record += "无得分记录,得分：0";
            return record;
        }
        for (MarketTaskItem item : marketTaskItems){
            Date deadLine = item.getMarketTask().getMarketTaskGroup().getDeadline();
            if(item.isFinished() == true && passDate(deadLine,item.getFinishDate()) == false){
                grade += 10;
                record += "检测任务项ID" + item.getMarketTaskItemId() + "：按时完成，+10分\n";
            }
            if((item.isFinished() == true && passDate(deadLine,item.getFinishDate()) == true)||
                    (item.isFinished() == false && passDate(deadLine,VirtualTime.getDate()) == true)){
                grade -= 10;
                record += "检测任务项ID" + item.getMarketTaskItemId() + "：未按时完成，-10分\n";
            }
            if ((item.isFinished() == true && passTwentyDays(deadLine,item.getFinishDate()) == true)||
                    (item.isFinished() == false && passTwentyDays(deadLine,VirtualTime.getDate()) == true)){
                grade -= 20;
                record += "检测任务项ID" + item.getMarketTaskItemId() + "：超20天未完成，-20分\n";
            }
        }
        if(record.equals("")){
            record += "无得分记录，得分：0";
        }else {
            record += "该市场总得分：" + grade;
        }
        return record;
    }

    public String grade(int marketTaskId){          //查看该市场得分情况
        Date time = VirtualTime.getDate();
        Optional<MarketTask> marketTaskOptional = marketTaskDao.findById(marketTaskId);
        MarketTask marketTask = marketTaskOptional.isPresent()?marketTaskOptional.get() : null;
        int grade = 0;
        String record = "";
        if(marketTask == null){
            record += "无得分记录,得分：0";
            return record;
        }
        Market market = marketTask.getMarket();
        MarketTaskGroup marketTaskGroup = marketTaskGroupDao.findById(marketTask.getMarketTaskGroup().getMarketTaskGroupId()).get();
        List<MarketTaskItem> marketTaskItemList = marketTask.getMarketTaskItems();


        if(hasUnfinishedItem(marketTask.getMarketTaskId())==false && getLatestDate(marketTask.getMarketTaskId()).compareTo(marketTaskGroup.getDeadline())!=1){
            grade +=10;
            record += market.getMarketName() + "按时完成，得分：" + grade + "\n";
//            System.out.println(market.getMarketName() +" 得分：" + grade);
        }

        if((hasUnfinishedItem(marketTask.getMarketTaskId())==false && getLatestDate(marketTask.getMarketTaskId()).getTime() - marketTaskGroup.getDeadline().getTime()>0)
           || (hasUnfinishedItem(marketTask.getMarketTaskId())==true && time.getTime() - marketTaskGroup.getDeadline().getTime() > 0) ){       //未按时完成
            grade -= 10;
            record += market.getMarketName() + "未按时完成，扣10分，得分：" + grade + "\n";
        }
        if((hasUnfinishedItem(marketTask.getMarketTaskId())==false &&getLatestDate(marketTask.getMarketTaskId()).getTime() - marketTaskGroup.getDeadline().getTime() > 1728000000)
            ||(hasUnfinishedItem(marketTask.getMarketTaskId())==true && time.getTime() - marketTaskGroup.getDeadline().getTime() > 1728000000)){  //完成时间超过20天
            grade -= 20;
            record += market.getMarketName() + "超20天未完成，扣20分，得分：" + grade + "\n";
        }
        if(record.equals("")){
            record += "无得分记录,得分：0";
        }
        return record;
    }

    public boolean hasUnfinishedItem(int marketTaskId){          //是否有未完成的类别
        MarketTask marketTask = marketTaskDao.findById(marketTaskId).get();
        List<MarketTaskItem> marketTaskItemList = marketTaskItemDao.findByMarketTask(marketTask);
        for (MarketTaskItem item : marketTaskItemList){
            if(item.isFinished() == false){
                return true;
            }
        }
        return false;
    }

    public Date getLatestDate(int marketTaskId){             //得到最后完成时间
        MarketTask marketTask = marketTaskDao.findById(marketTaskId).get();
        List<MarketTaskItem> marketTaskItemList = marketTask.getMarketTaskItems();
        String s = "1999-01-01 00:00:00";
        Date date;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            date = simpleDateFormat.parse(s);

            for(MarketTaskItem item : marketTaskItemList){
                if(item.getFinishDate()!=null && item.getFinishDate().compareTo(date)==1){
                    date = item.getFinishDate();
                }
            }
            return date;
        }catch (ParseException e){
            e.printStackTrace();
        }
        return null;
    }
    public boolean passDate(Date endTime, Date date){         //是否超时
        if(date.compareTo(endTime)<=0){
            return false;
        }
        return true;
    }

    public boolean passTwentyDays(Date endTime, Date date){         //是否超过20天
        if( date.getTime() - endTime.getTime() > 1728000000){
            return true;
        }
        return false;
    }
}
