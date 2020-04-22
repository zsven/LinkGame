package swu.xl.linkgame.Activity;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.BounceInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gyf.immersionbar.ImmersionBar;

import org.litepal.LitePal;

import java.text.DecimalFormat;
import java.util.List;

import swu.xl.linkgame.Constant.Constant;
import swu.xl.linkgame.Constant.Enum.PropMode;
import swu.xl.linkgame.LinkGame.Utils.AnimalSearchUtil;
import swu.xl.linkgame.LinkGame.SelfView.AnimalView;
import swu.xl.linkgame.LinkGame.Constant.LinkConstant;
import swu.xl.linkgame.LinkGame.Model.LinkInfo;
import swu.xl.linkgame.LinkGame.Manager.LinkManager;
import swu.xl.linkgame.LinkGame.Utils.LinkUtil;
import swu.xl.linkgame.LinkGame.SelfView.XLRelativeLayout;
import swu.xl.linkgame.Model.XLLevel;
import swu.xl.linkgame.Model.XLProp;
import swu.xl.linkgame.Model.XLUser;
import swu.xl.linkgame.R;
import swu.xl.linkgame.Util.ScreenUtil;

public class LinkActivity extends AppCompatActivity implements View.OnClickListener,LinkManager.LinkGame {
    //屏幕宽度
    int screenWidth;

    //当前关卡模型数据
    XLLevel level;

    //用户
    XLUser user;

    //道具
    List<XLProp> props;

    //AnimalView的容器
    XLRelativeLayout link_layout;

    //存储点的信息集合
    LinkInfo linkInfo;

    //游戏管理者
    LinkManager manager;

    //显示关卡的文本
    TextView level_text;
    //显示金币的文本
    TextView money_text;
    //显示时间的文本
    TextView time_text;

    //拳头道具
    View prop_fight;
    //炸弹道具
    View prop_bomb;
    //刷新道具
    View prop_refresh;

    //显示拳头道具数量的文本
    TextView fight_num_text;

    //显示炸弹道具数量的文本
    TextView bomb_num_text;

    //显示刷新道具数量的文本
    TextView refresh_num_text;

    //记录金币的变量
    int money;

    //记录拳头道具的数量
    int fight_num;

    //记录炸弹道具的数量
    int bomb_num;

    //记录刷新道具的数量
    int refresh_num;

    //暂停
    ImageView pause;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link);

        //沉浸式状态栏
        ImmersionBar.with(this).barAlpha(1.0f).init();

        //加载数据
        initData();

        //加载视图
        initView();

        //开始游戏
        manager.startGame(this,
                link_layout,screenWidth,
                level.getL_id(),
                level.getL_mode()
        );

        //监听触摸事件
        link_layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //获取触摸点相对于布局的坐标
                int x = (int) event.getX();
                int y = (int) event.getY();

                //触摸事件
                if (event.getAction() == MotionEvent.ACTION_DOWN){
                    for (final AnimalView animal : manager.getAnimals()) {
                        //获取AnimalView实例的rect
                        RectF rectF = new RectF(
                                animal.getLeft(),
                                animal.getTop(),
                                animal.getRight(),
                                animal.getBottom());

                        //判断是否包含
                        if (rectF.contains(x,y) && animal.getVisibility() == View.VISIBLE){
                            //获取上一次触摸的AnimalView
                            final AnimalView lastAnimal = manager.getLastAnimal();

                            //如果不是第一次触摸 且 触摸的不是同一个点
                            if (lastAnimal != null && lastAnimal != animal){

                                Log.d(Constant.TAG,lastAnimal+" "+animal);

                                //如果两者的图片相同，且两者可以连接
                                if(animal.getFlag() == lastAnimal.getFlag() &&
                                        AnimalSearchUtil.canMatchTwoAnimalWithTwoBreak(
                                        manager.getBoard(),
                                        lastAnimal.getPoint(),
                                        animal.getPoint(),
                                        linkInfo
                                )){
                                    //当前点改变背景和动画
                                    animal.changeAnimalBackground(LinkConstant.ANIMAL_SELECT_BG);
                                    animationOnSelectAnimal(animal);

                                    //画线
                                    link_layout.setLinkInfo(linkInfo);

                                    //延迟操作
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            //修改模板
                                            manager.getBoard()[lastAnimal.getPoint().x][lastAnimal.getPoint().y] = 0;
                                            manager.getBoard()[animal.getPoint().x][animal.getPoint().y] = 0;

                                            //输出模板
                                            for (int i = 0; i < manager.getBoard().length; i++) {
                                                for (int j = 0; j < manager.getBoard()[0].length; j++) {
                                                    System.out.print(manager.getBoard()[i][j]+" ");
                                                }
                                                System.out.println("");
                                            }

                                            //隐藏
                                            lastAnimal.setVisibility(View.INVISIBLE);
                                            lastAnimal.clearAnimation();
                                            animal.setVisibility(View.INVISIBLE);
                                            animal.clearAnimation();

                                            //上一个点置空
                                            manager.setLastAnimal(null);

                                            //去线
                                            link_layout.setLinkInfo(null);

                                            //获得金币
                                            money += 2;
                                            money_text.setText(String.valueOf(money));
                                        }
                                    },500);
                                }else {
                                    //否则

                                    //上一个点恢复原样
                                    lastAnimal.changeAnimalBackground(LinkConstant.ANIMAL_BG);
                                    if (lastAnimal.getAnimation() != null){
                                        //清楚所有动画
                                        lastAnimal.clearAnimation();
                                    }

                                    //设置当前点的背景颜色和动画
                                    animal.changeAnimalBackground(LinkConstant.ANIMAL_SELECT_BG);
                                    animationOnSelectAnimal(animal);

                                    //将当前点作为选中点
                                    manager.setLastAnimal(animal);
                                }
                            }else if (lastAnimal == null){
                                //第一次触摸 当前点改变背景和动画
                                animal.changeAnimalBackground(LinkConstant.ANIMAL_SELECT_BG);
                                animationOnSelectAnimal(animal);

                                //将当前点作为选中点
                                manager.setLastAnimal(animal);
                            }
                        }
                    }
                }

                return true;
            }
        });
    }

    /**
     * 加载数据
     */
    private void initData() {
        //获取数据
        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();
        assert bundle != null;
        level = bundle.getParcelable("level");

        Log.d(Constant.TAG,"--------");
        Log.d(Constant.TAG, String.valueOf(level));

        //查询用户数据
        List<XLUser> users = LitePal.findAll(XLUser.class);
        user = users.get(0);
        money = user.getU_money();

        //查询道具数据
        props = LitePal.findAll(XLProp.class);
        for (XLProp prop : props) {
            if (prop.getP_kind() == PropMode.PROP_FIGHT.getValue()){
                //拳头道具
                fight_num = prop.getP_number();
            }else if (prop.getP_kind() == PropMode.PROP_BOMB.getValue()){
                //炸弹道具
                bomb_num = prop.getP_number();
            }else {
                //刷新道具
                refresh_num = prop.getP_number();
            }
        }
    }

    /**
     * 加载视图
     */
    private void initView() {
        screenWidth = ScreenUtil.getScreenWidth(getApplicationContext());

        link_layout = findViewById(R.id.link_layout);

        linkInfo = new LinkInfo();

        manager = LinkManager.getLinkManager();

        level_text = findViewById(R.id.link_level_text);
        level_text.setText(String.valueOf(level.getL_id()));
        money_text = findViewById(R.id.link_money_text);
        time_text = findViewById(R.id.link_time_text);

        prop_fight = findViewById(R.id.prop_fight);
        prop_fight.setOnClickListener(this);
        prop_bomb = findViewById(R.id.prop_bomb);
        prop_bomb.setOnClickListener(this);
        prop_refresh = findViewById(R.id.prop_refresh);
        prop_refresh.setOnClickListener(this);
        pause = findViewById(R.id.link_pause);
        pause.setOnClickListener(this);

        manager.setListener(this);

        //找到显示道具数量的控件
        fight_num_text = findViewById(R.id.link_prop_fight_text);
        bomb_num_text = findViewById(R.id.link_prop_bomb_text);
        refresh_num_text = findViewById(R.id.link_prop_refresh_text);

        //设置金币
        money_text.setText(String.valueOf(money));

        //设置道具数量
        fight_num_text.setText(String.valueOf(fight_num));
        bomb_num_text.setText(String.valueOf(bomb_num));
        refresh_num_text.setText(String.valueOf(refresh_num));
    }

    /**
     * 选中的AnimalView动画
     * @param animal
     */
    private void animationOnSelectAnimal(AnimalView animal){
        //缩放动画
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                1.0f, 1.05f,
                1.0f, 1.05f,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
        );
        scaleAnimation.setDuration(100);
        scaleAnimation.setRepeatCount(0);
        scaleAnimation.setFillAfter(true);


        //旋转动画
        RotateAnimation rotateAnimation = new RotateAnimation(
                -20f,
                20f,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
        );
        rotateAnimation.setDuration(500);
        rotateAnimation.setStartOffset(100);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        rotateAnimation.setRepeatMode(Animation.REVERSE);
        rotateAnimation.setInterpolator(new BounceInterpolator());

        //组合动画
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(scaleAnimation);
        animationSet.addAnimation(rotateAnimation);

        //开启动画
        animal.startAnimation(animationSet);
        animationSet.startNow();
    }

    //点击事件
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.prop_fight:
                Log.d(Constant.TAG,"拳头道具");

                if (fight_num > 0){
                    //随机消除一对可以消除的AnimalView
                    manager.fightGame();

                    //数量减1
                    fight_num--;
                    fight_num_text.setText(String.valueOf(fight_num));
                }else {
                    Toast.makeText(this, "道具已经用完", Toast.LENGTH_SHORT).show();
                }

                break;
            case R.id.prop_bomb:
                Log.d(Constant.TAG,"炸弹道具");

                if (bomb_num > 0){
                    //随机消除某一种所有的AnimalView
                    manager.bombGame();

                    //数量减1
                    bomb_num--;
                    bomb_num_text.setText(String.valueOf(bomb_num));
                }else {
                    Toast.makeText(this, "道具已经用完", Toast.LENGTH_SHORT).show();
                }

                break;
            case R.id.prop_refresh:
                Log.d(Constant.TAG,"刷新道具");

                if (refresh_num > 0){
                    //刷新游戏
                    manager.refreshGame(
                            getApplicationContext(),
                            link_layout,
                            screenWidth,
                            level.getL_id(),
                            level.getL_mode());

                    //数量减1
                    refresh_num--;
                    refresh_num_text.setText(String.valueOf(refresh_num));
                }else {
                    Toast.makeText(this, "道具已经用完", Toast.LENGTH_SHORT).show();
                }

                break;
            case R.id.link_pause:
                Log.d(Constant.TAG,"暂停");

                //暂停游戏
                manager.pauseGame();

                break;
        }
    }

    //时间发生改变的时间
    @SuppressLint("SetTextI18n")
    @Override
    public void onTimeChanged(float time) {
        //如果时间小于0
        if (time <= 0.0){
            manager.pauseGame();
            manager.endGame(this,level,time);
        }else {
            //保留小数后一位
            time_text.setText(new DecimalFormat("##0.0").format(time)+"秒");
        }

        //如果board全部清除了
        if (LinkUtil.getBoardState()){
            //结束游戏
            manager.pauseGame();
            level.setL_time((int) (LinkConstant.TIME -time));
            level.setL_new(LinkUtil.getStarByTime((int) time));
            manager.endGame(this,level,time);

            //关卡结算
            level.update(level.getId());
            XLLevel next_level = new XLLevel();
            next_level.setL_new('4');
            next_level.update(level.getId()+1);

            //金币道具清算
            user.setU_money(money);
            user.update(1);
            for (XLProp prop : props) {
                if (prop.getP_kind() == PropMode.PROP_FIGHT.getValue()){
                    //拳头道具
                    prop.setP_number(fight_num);
                    prop.update(1);
                }else if (prop.getP_kind() == PropMode.PROP_BOMB.getValue()){
                    //炸弹道具
                    prop.setP_number(bomb_num);
                    prop.update(2);
                }else {
                    //刷新道具
                    prop.setP_number(refresh_num);
                    prop.update(3);
                }
            }

        }
    }
}
