package com.zhuoan.queue;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.event.BaseEventDeal;
import com.zhuoan.biz.event.bdx.BDXGameEventDealNew;
import com.zhuoan.biz.event.circle.CircleBaseEventDeal;
import com.zhuoan.biz.event.club.ClubEventDeal;
import com.zhuoan.biz.event.ddz.DdzGameEventDeal;
import com.zhuoan.biz.event.gdy.GDYGameEventDeal;
import com.zhuoan.biz.event.gppj.GPPJGameEventDeal;
import com.zhuoan.biz.event.gzmj.GzMjGameEventDeal;
import com.zhuoan.biz.event.match.MatchEventDeal;
import com.zhuoan.biz.event.nn.NNGameEventDealNew;
import com.zhuoan.biz.event.pdk.PDKGameEventDeal;
import com.zhuoan.biz.event.qzmj.QZMJGameEvent;
import com.zhuoan.biz.event.qzmj.QZMJGameEventDeal;
import com.zhuoan.biz.event.sg.SGGameEventDeal;
import com.zhuoan.biz.event.sss.SSSGameEventDealNew;
import com.zhuoan.biz.event.sw.SwGameEventDeal;
import com.zhuoan.biz.event.tea.TeaEventDeal;
import com.zhuoan.biz.event.zjh.ZJHGameEventDealNew;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.qzmj.QZMJGameRoom;
import com.zhuoan.biz.robot.RobotEventDeal;
import com.zhuoan.constant.event.enmu.PDKEventEnum;
import com.zhuoan.constant.event.enmu.TeaEventEnum;
import com.zhuoan.exception.EventException;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.service.socketio.impl.GameMain;
import java.util.UUID;
import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GameEventDeal {
    private static final Logger log = LoggerFactory.getLogger(GameEventDeal.class);
    @Resource
    private BaseEventDeal baseEventDeal;
    @Resource
    private NNGameEventDealNew nnGameEventDealNew;
    @Resource
    private SSSGameEventDealNew sssGameEventDealNew;
    @Resource
    private ZJHGameEventDealNew zjhGameEventDealNew;
    @Resource
    private BDXGameEventDealNew bdxGameEventDealNew;
    @Resource
    private QZMJGameEventDeal qzmjGameEventDeal;
    @Resource
    private GPPJGameEventDeal gppjGameEventDeal;
    @Resource
    private SwGameEventDeal swGameEventDeal;
    @Resource
    private DdzGameEventDeal ddzGameEventDeal;
    @Resource
    private PDKGameEventDeal pdkGameEventDeal;
    @Resource
    private MatchEventDeal matchEventDeal;
    @Resource
    private ClubEventDeal clubEventDeal;
    @Resource
    private CircleBaseEventDeal circleBaseEventDeal;
    @Resource
    private TeaEventDeal teaEventDeal;
    @Resource
    private GDYGameEventDeal gdyGameEventDeal;
    @Resource
    private SGGameEventDeal sgGameEventDeal;
    @Resource
    private GzMjGameEventDeal gzMjGameEventDeal;
    @Resource
    private RedisInfoService redisInfoService;
    @Resource
    private RobotEventDeal robotEventDeal;

    public GameEventDeal() {
    }

    public void eventsMQ(Message message) {
        if (null == GameMain.server) {
        }

        JSONObject jsonObject = JSONObject.fromObject(this.obtainMessageStr(message));
        Object data = jsonObject.get("dataObject");
        Integer gid = (Integer)jsonObject.get("gid");
        Integer sorts = (Integer)jsonObject.get("sorts");
        String sessionId = (String)jsonObject.get("sessionId");
        if (jsonObject.containsKey("idempotentUUID")) {
            String key = "idempotentUUID" + ":" + sessionId + ":" + gid + ":" + sorts + ":" + jsonObject.get("idempotentUUID");
            if (!this.redisInfoService.idempotent(key)) {
                return;
            }
        }

        SocketIOClient client = GameMain.server.getClient(UUID.fromString(sessionId));
        JSONObject o = null;
        if (null != data) {
            o = JSONObject.fromObject(data);
            if (null != o && o.containsKey("uid")) {
                String userId = o.getString("uid");
                RoomManage.channelMap.put(userId, sessionId);
                RoomManage.sessionIdMap.put(sessionId, userId);
            }
        }
        switch(gid) {
        
            case 0:
                this.baseEvents(data, sorts, client);
                break;
            case 1:
            	
                this.NNEvents(data, sorts, client);
                break;
            case 2:
                this.ddzEvents(data, sorts, client);	
                break;
            case 3:
                this.QzMjEvents(data, sorts, client);
                break;
            case 4:
                this.SSSEvents(data, sorts, client);
                break;
            case 5:
                this.GpPjEvents(data, sorts, client);
                break;
            case 6:
                this.ZJHEvents(data, sorts, client);
                break;
            case 10:
                this.BDZEvents(data, sorts, client);
                break;
            case 14:
                this.swEvents(data, sorts, client);
                break;
            case 18:
                if (null != o) {
                    this.gdyEvents(o, sorts, client);
                }
                break;
            case 19:
                if (null != o) {
                    this.sgEvents(o, sorts, client);
                }
                break;
            case 20:
                this.gzmjEvents(data, sorts, client);
                break;
            case 21:
                if (null != o) {
                    this.pdkEvents(o, sorts, client);
                }
                break;
            case 100:
                this.matchEvents(data, sorts, client);
                break;
            case 101:
                this.clubEvents(data, sorts, client);
                break;
            case 200:
                if (null != o) {
                    this.circleBaseEvents(o, sorts, client);
                }
                break;
            case 210:
                if (null != o) {
                    this.teaEvents(o, sorts, client);
                }
                break;
            case 300:
            	if (null!=o) {
					this.robotEventsDeal(o,sorts,client);
				}
        }

    }

    private void robotEventsDeal(JSONObject object, Integer sorts, SocketIOClient client) {
    	switch (sorts) {
		case 0:
			robotEventDeal.robotJoinRoom(client,object);
			break;

		default:
			break;
		}
    }
    
    private void circleBaseEvents(JSONObject object, Integer sorts, SocketIOClient client) {
        switch(sorts) {
            case 0:
                this.circleBaseEventDeal.getCircleBaseTest(client, object);
                break;
            case 1:
                this.circleBaseEventDeal.createModifyCircleEvent(client, object);
            case 2:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
            case 48:
            case 49:
            case 50:
            case 51:
            case 52:
            case 53:
            case 54:
            case 55:
            case 56:
            case 57:
            case 58:
            case 59:
            case 60:
            case 61:
            case 62:
            case 63:
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
            case 79:
            case 80:
            case 81:
            case 82:
            case 83:
            case 84:
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 92:
            case 93:
            case 94:
            case 95:
            case 96:
            case 97:
            case 98:
            case 99:
            default:
                break;
            case 3:
                this.circleBaseEventDeal.dismissCircleEvent(client, object);
                break;
            case 4:
                this.circleBaseEventDeal.mgrSettingCircleEvent(client, object);
                break;
            case 5:
                this.circleBaseEventDeal.transferCircleEvent(client, object);
                break;
            case 6:
                this.circleBaseEventDeal.circleMbrAddExitExamEvent(client, object);
                break;
            case 7:
                this.circleBaseEventDeal.circleBlacklistEvent(client, object);
                break;
            case 8:
                this.circleBaseEventDeal.circleFundSavePayEvent(client, object);
                break;
            case 9:
                this.circleBaseEventDeal.circleFundBillDetailEvent(client, object);
                break;
            case 10:
                this.circleBaseEventDeal.circleMessageInfoEvent(client, object);
                break;
            case 11:
                this.circleBaseEventDeal.circleMessageBlanceReminderEvent(client, object);
                break;
            case 12:
                this.circleBaseEventDeal.circlePartnerListEvent(client, object);
                break;
            case 13:
                this.circleBaseEventDeal.circlePartnerAddEvent(client, object);
                break;
            case 14:
                this.circleBaseEventDeal.circleMemberListEvent(client, object);
                break;
            case 15:
                this.circleBaseEventDeal.circleModifyStrenthEvent(client, object);
                break;
            case 16:
                this.circleBaseEventDeal.circleStrenthLogEvent(client, object);
                break;
            case 17:
                this.circleBaseEventDeal.circlePartnerShareEvent(client, object);
                break;
            case 18:
                this.circleBaseEventDeal.circleGeneralJoinExitEvent(client, object);
                break;
            case 19:
                this.circleBaseEventDeal.circleListEvent(client, object);
                break;
            case 20:
                this.circleBaseEventDeal.circleMbrExamListEvent(client, object);
                break;
            case 21:
                this.circleBaseEventDeal.circleRecordEvent(client, object);
                break;
            case 22:
                this.circleBaseEventDeal.circleExamMsgInfoEvent(client, object);
                break;
            case 23:
                this.circleBaseEventDeal.addCircleMemberEvent(client, object);
                break;
            case 24:
                this.circleBaseEventDeal.historyHpChangeByOperateEvent(client, object);
                break;
            case 25:
                this.circleBaseEventDeal.getMemberDetailInfo(client, object);
                break;
            case 26:
                this.circleBaseEventDeal.circleRoomListEvent(client, object);
                break;
            case 27:
                this.circleBaseEventDeal.circleDelMemberEvent(client, object);
                break;
            case 28:
                this.circleBaseEventDeal.getSysGlobalEvent(client, object);
                break;
            case 29:
                this.circleBaseEventDeal.circleGetProfitBalanceToHpEvent(client, object);
                break;
            case 30:
                this.circleBaseEventDeal.getGameCircleInfoEvent(client, object);
                break;
            case 31:
                this.circleBaseEventDeal.circleRecordInfoEvent(client, object);
                break;
            case 32:
                this.circleBaseEventDeal.quickJoinRoomCircleEvent(client, object);
                break;
            case 33:
                this.circleBaseEventDeal.removeRoomCircleEvent(client, object);
                break;
            case 34:
                this.circleBaseEventDeal.circleMemberInfoListEvent(client, object);
                break;
            case 35:
                this.circleBaseEventDeal.offlineCircleEvent(client, object);
                break;
            case 36:
                this.circleBaseEventDeal.quickEntryRoomCircleEvent(client, object);
                break;
            case 37:
                this.circleBaseEventDeal.resetPartnerEvent(client, object);
                break;
            case 38:
                this.circleBaseEventDeal.readMessageEvent(client, object);
                break;
            case 39:
                this.circleBaseEventDeal.circleVisitJoinRoomEvent(client, object);
                break;
            case 40:
                this.circleBaseEventDeal.circleVisitJoinGameEvent(client, object);
                break;
            case 41:
                this.circleBaseEventDeal.circleVisitListEvent(client, object);
                break;
            case 100:
                this.circleBaseEventDeal.subscribeCircleListEvent(client, object);
                break;
            case 101:
                this.circleBaseEventDeal.unSubscribeCircleListEvent(client);
                break;
            case 201:
            	System.out.println("监听================");
                this.circleBaseEventDeal.goBank(client, object);
                break;
            case 202:
            	System.out.println("监听================");
            	this.circleBaseEventDeal.operationMoneyEvent(client,object);
            	break;
            case 203:
            	System.out.println("监听到消息");
            	this.circleBaseEventDeal.luckyDraw(client, object);
            	break;
            
        }

    }

    private void teaEvents(JSONObject object, Integer sorts, SocketIOClient client) {
        object.discard("token");
        if (null != sorts) {
            if (sorts == TeaEventEnum.TEST.getType()) {
                this.teaEventDeal.teaTest(client, object, TeaEventEnum.TEST.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_LIST.getType()) {
                this.teaEventDeal.listEvent(client, object, TeaEventEnum.TEA_LIST.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_CREATOR.getType()) {
            	System.out.println("茶楼创建房间");
                this.teaEventDeal.creatorEvent(client, object, TeaEventEnum.TEA_CREATOR.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_JOIN.getType()) {
                this.teaEventDeal.joinEvent(client, object, TeaEventEnum.TEA_JOIN.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_MODIFY_NAME.getType()) {
                this.teaEventDeal.modifyNameEvent(client, object, TeaEventEnum.TEA_MODIFY_NAME.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_MODIFY_PLAY.getType()) {
                this.teaEventDeal.modifyPlayEvent(client, object, TeaEventEnum.TEA_MODIFY_PLAY.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_ADD_MEMBER.getType()) {
                this.teaEventDeal.addMemberEvent(client, object, TeaEventEnum.TEA_ADD_MEMBER.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_IMPORT_MEMBER_LIST.getType()) {
                this.teaEventDeal.importMemberListEvent(client, object, TeaEventEnum.TEA_IMPORT_MEMBER_LIST.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_IMPORT_MEMBER.getType()) {
                this.teaEventDeal.importMemberEvent(client, object, TeaEventEnum.TEA_IMPORT_MEMBER.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_CLOSES.getType()) {
                this.teaEventDeal.closesEvent(client, object, TeaEventEnum.TEA_CLOSES.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_REMOVE.getType()) {
                this.teaEventDeal.removeEvent(client, object, TeaEventEnum.TEA_REMOVE.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_MEMBER_LIST.getType()) {
                this.teaEventDeal.memberListEvent(client, object, TeaEventEnum.TEA_MEMBER_LIST.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_AUDIT_MEMBER.getType()) {
                this.teaEventDeal.auditMemberEvent(client, object, TeaEventEnum.TEA_AUDIT_MEMBER.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_AUDIT_MEMBER_LIST.getType()) {
                this.teaEventDeal.auditMemberListEvent(client, object, TeaEventEnum.TEA_AUDIT_MEMBER_LIST.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_EXIT_INFO_LIST.getType()) {
                this.teaEventDeal.exitInfoListEvent(client, object, TeaEventEnum.TEA_EXIT_INFO_LIST.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_ADMIN_LIST.getType()) {
                this.teaEventDeal.adminListEvent(client, object, TeaEventEnum.TEA_ADMIN_LIST.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_MODIFY_ADMIN.getType()) {
                this.teaEventDeal.modifyAdminEvent(client, object, TeaEventEnum.TEA_MODIFY_ADMIN.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_MY_RECORD_LIST.getType()) {
                this.teaEventDeal.myRecordListEvent(client, object, TeaEventEnum.TEA_MY_RECORD_LIST.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_RECORD_LIST.getType()) {
                this.teaEventDeal.recordListEvent(client, object, TeaEventEnum.TEA_RECORD_LIST.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_RECORD_STATIS_LIST.getType()) {
                this.teaEventDeal.recordStatisListEvent(client, object, TeaEventEnum.TEA_RECORD_STATIS_LIST.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_BIG_WINNER_LIST.getType()) {
                this.teaEventDeal.bigWinnerListEvent(client, object, TeaEventEnum.TEA_BIG_WINNER_LIST.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_QUICK_JOIN.getType()) {
                this.teaEventDeal.quickJoinEvent(client, object, TeaEventEnum.TEA_QUICK_JOIN.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_INFO.getType()) {
                this.teaEventDeal.infoEvent(client, object, TeaEventEnum.TEA_INFO.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_EXIT_MEMBER.getType()) {
                this.teaEventDeal.exitMemberEvent(client, object, TeaEventEnum.TEA_EXIT_MEMBER.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_MODIFY_INFO_RECORD.getType()) {
                this.teaEventDeal.modifyInfoRecordEvent(client, object, TeaEventEnum.TEA_MODIFY_INFO_RECORD.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_ROOM_LIST.getType()) {
                this.teaEventDeal.roomListEvent(client, object, TeaEventEnum.TEA_ROOM_LIST.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_REMOVE_ROOM.getType()) {
                this.teaEventDeal.removeRoomEvent(client, object, TeaEventEnum.TEA_REMOVE_ROOM.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_DEL_MEMBER.getType()) {
                this.teaEventDeal.delMemberEvent(client, object, TeaEventEnum.TEA_DEL_MEMBER.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_JOIN_ROOM.getType()) {
                this.teaEventDeal.joinRoomEvent(client, object, TeaEventEnum.TEA_JOIN_ROOM.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_QUIT_ROOM.getType()) {
                this.teaEventDeal.quitRoomEvent(client, object, TeaEventEnum.TEA_QUIT_ROOM.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_RECORD_INFO.getType()) {
                this.teaEventDeal.recordInfoEvent(client, object, TeaEventEnum.TEA_RECORD_INFO.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_MODIFY_PROXY_MONEY.getType()) {
                this.teaEventDeal.modifyProxyMoneyEvent(client, object, TeaEventEnum.TEA_MODIFY_PROXY_MONEY.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_MODIFY_TEA_MONEY.getType()) {
                this.teaEventDeal.modifyTeaMoneyEvent(client, object, TeaEventEnum.TEA_MODIFY_TEA_MONEY.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_GET_MEMBER_INFO.getType()) {
                this.teaEventDeal.getMemberInfoEvent(client, object, TeaEventEnum.TEA_GET_MEMBER_INFO.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_MEMBER_MSG_READ.getType()) {
                this.teaEventDeal.memberMsgReadEvent(client, object, TeaEventEnum.TEA_MEMBER_MSG_READ.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_MONEY_RECORD_LIST.getType()) {
                this.teaEventDeal.moneyRecordListEvent(client, object, TeaEventEnum.TEA_MONEY_RECORD_LIST.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_RECORD_TICK.getType()) {
                this.teaEventDeal.recordTickEvent(client, object, TeaEventEnum.TEA_RECORD_TICK.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_MODIFY_MEMBER_SCORE_LOCK.getType()) {
                this.teaEventDeal.modifyMemberScoreLockEvent(client, object, TeaEventEnum.TEA_MODIFY_MEMBER_SCORE_LOCK.getEventPush());
            } else if (sorts == TeaEventEnum.TEA_MODIFY_MEMBER_USE_STATUS.getType()) {
                this.teaEventDeal.modifyMemberUseStatus(client, object, TeaEventEnum.TEA_MODIFY_MEMBER_USE_STATUS.getEventPush());
            }
        }
    }

    private void clubEvents(Object data, Integer sorts, SocketIOClient client) {
        switch(sorts) {
            case 2:
                this.clubEventDeal.getMyClubList(client, data);
                break;
            case 3:
                this.clubEventDeal.getClubMembers(client, data);
                break;
            case 4:
                this.clubEventDeal.getClubSetting(client, data);
                break;
            case 5:
                this.clubEventDeal.changeClubSetting(client, data);
                break;
            case 6:
                this.clubEventDeal.exitClub(client, data);
                break;
            case 7:
                this.clubEventDeal.toTop(client, data);
                break;
            case 8:
                this.clubEventDeal.refreshClubInfo(client, data);
                break;
            case 9:
                this.clubEventDeal.quickJoinClubRoom(client, data);
                break;
            case 10:
                this.clubEventDeal.getClubApplyList(client, data);
                break;
            case 11:
                this.clubEventDeal.clubApplyReview(client, data);
                break;
            case 12:
                this.clubEventDeal.clubLeaderInvite(client, data);
                break;
            case 13:
                this.clubEventDeal.clubLeaderOut(client, data);
                break;
            case 14:
                this.clubEventDeal.createClub(client, data);
                break;
            case 15:
                this.clubEventDeal.clubRecharge(client, data);
        }

    }

    private void matchEvents(Object data, Integer sorts, SocketIOClient client) {
        switch(sorts) {
            case 1:
                this.matchEventDeal.obtainMatchInfo(client, data);
                break;
            case 2:
                this.matchEventDeal.matchSignUp(client, data);
                break;
            case 3:
                this.matchEventDeal.updateMatchCount(client, data);
                break;
            case 4:
                this.matchEventDeal.matchCancelSign(client, data);
                break;
            case 5:
                this.matchEventDeal.getWinningRecord(client, data);
                break;
            case 6:
                this.matchEventDeal.getSignUpInfo(client, data);
                break;
            case 7:
                this.matchEventDeal.checkMatchStatus(client);
        }

    }

    private void ddzEvents(Object data, Integer sorts, SocketIOClient client) {
        switch(sorts) {
            case 1:
                this.ddzGameEventDeal.gameReady(client, data);
                break;
            case 2:
                this.ddzGameEventDeal.gameBeLandlord(client, data);
                break;
            case 3:
                this.ddzGameEventDeal.gameEvent(client, data);
                break;
            case 4:
                this.ddzGameEventDeal.reconnectGame(client, data);
                break;
            case 5:
                this.ddzGameEventDeal.gamePrompt(client, data);
                break;
            case 6:
                this.ddzGameEventDeal.gameContinue(client, data);
                break;
            case 7:
                this.ddzGameEventDeal.exitRoom(client, data);
                break;
            case 8:
                this.ddzGameEventDeal.closeRoom(client, data);
                break;
            case 9:
                this.ddzGameEventDeal.gameTrustee(client, data);
                break;
            case 10:
                this.ddzGameEventDeal.gameAutoPlay(client, data);
            case 11:
            case 12:
            default:
                break;
            case 13:
                this.ddzGameEventDeal.getOutInfo(client, data);
                break;
            case 14:
                this.ddzGameEventDeal.gameDouble(client, data);
        }

    }

    private void pdkEvents(JSONObject object, Integer sorts, SocketIOClient client) {
        object.discard("token");
        if (null != sorts) {
            PDKEventEnum[] var4 = PDKEventEnum.values();
            int var5 = var4.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                PDKEventEnum m = var4[var6];
                if (sorts == m.getType()) {
                    if (sorts == PDKEventEnum.TEST.getType()) {
                        this.pdkGameEventDeal.pdkTest(client, object, m.getEventPush());
                        return;
                    }

                    if (sorts == PDKEventEnum.PDK_GAME_READY.getType()) {
                        this.pdkGameEventDeal.gameReadyEvent(client, object, m.getEventPush());
                        return;
                    }

                    if (sorts == PDKEventEnum.PDK_GAME.getType()) {
                        this.pdkGameEventDeal.gameEvent(client, object, m.getEventPush());
                        return;
                    }

                    if (sorts == PDKEventEnum.PDK_RECONNECT_GAME.getType()) {
                        this.pdkGameEventDeal.reconnectGameEvent(client, object, m.getEventPush());
                        return;
                    }

                    if (sorts == PDKEventEnum.PDK_EXIT_ROOM.getType()) {
                        this.pdkGameEventDeal.exitRoomeEvent(client, object, m.getEventPush());
                        return;
                    }

                    if (sorts == PDKEventEnum.PDK_CLOSE_ROOM.getType()) {
                        this.pdkGameEventDeal.closeRoomEvent(client, object, m.getEventPush());
                        return;
                    }

                    if (sorts == PDKEventEnum.PDK_GAME_TRUSTEE.getType()) {
                        this.pdkGameEventDeal.gameTrusteeEvent(client, object, m.getEventPush());
                        return;
                    }

                    if (sorts == PDKEventEnum.PDK_GAME_TIP.getType()) {
                        this.pdkGameEventDeal.gameTipEvent(client, object, m.getEventPush());
                        return;
                    }

                    if (sorts == PDKEventEnum.PDK_GAME_FORCED_ROOM_EVENT.getType()) {
                        this.pdkGameEventDeal.gameForcedRoomEvent(object);
                        return;
                    }
                }
            }

        }
    }

    private void swEvents(Object data, Integer sorts, SocketIOClient client) {
        switch(sorts) {
            case 1:
                this.swGameEventDeal.gameStart(client, data);
                break;
            case 2:
                this.swGameEventDeal.gameBet(client, data);
                break;
            case 3:
                this.swGameEventDeal.gameBeBanker(client, data);
                break;
            case 4:
                this.swGameEventDeal.gameUndo(client, data);
                break;
            case 5:
                this.swGameEventDeal.exitRoom(client, data);
                break;
            case 6:
                this.swGameEventDeal.gameChangeSeat(client, data);
                break;
            case 7:
                this.swGameEventDeal.reconnectGame(client, data);
                break;
            case 8:
                this.swGameEventDeal.getHistory(client, data);
                break;
            case 9:
                this.swGameEventDeal.getAllUser(client, data);
                break;
            case 10:
                this.swGameEventDeal.gameHide(client, data);
                break;
            case 11:
                this.swGameEventDeal.getUndoInfo(client, data);
        }

    }

    private void GpPjEvents(Object data, Integer sorts, SocketIOClient client) {
        switch(sorts) {
            case 1:
                this.gppjGameEventDeal.gameReady(client, data);
                break;
            case 2:
                this.gppjGameEventDeal.gameStart(client, data);
                break;
            case 3:
                this.gppjGameEventDeal.gameCut(client, data);
                break;
            case 4:
                this.gppjGameEventDeal.gameQz(client, data);
                break;
            case 5:
                this.gppjGameEventDeal.gameXz(client, data);
                break;
            case 6:
                this.gppjGameEventDeal.gameShow(client, data);
                break;
            case 7:
                this.gppjGameEventDeal.reconnectGame(client, data);
                break;
            case 8:
                this.gppjGameEventDeal.exitRoom(client, data);
                break;
            case 9:
                this.gppjGameEventDeal.closeRoom(client, data);
                break;
            case 10:
                this.gppjGameEventDeal.reShuffle(client, data);
        }

    }

    private void QzMjEvents(Object data, Integer sorts, SocketIOClient client) {
        switch(sorts) {
            case 1:
                this.qzmjGameEventDeal.loadFinish(client, data);
                break;
            case 2:
                this.qzmjGameEventDeal.gameChuPai(client, data);
                break;
            case 3:
                this.qzmjGameEventDeal.gameEvent(client, data);
                break;
            case 4:
                this.qzmjGameEventDeal.gangChupaiEvent(client, data);
                break;
            case 5:
                this.qzmjGameEventDeal.closeRoom(client, data);
                break;
            case 6:
                this.qzmjGameEventDeal.exitRoom(client, data);
                break;
            case 7:
                this.qzmjGameEventDeal.reconnectGame(client, data);
                break;
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            	JSONObject postData = JSONObject.fromObject(data);
            	String account = postData.getString("account");
            	String status = postData.getString("status");
            	if (QZMJGameRoom.jinjinAccount.contains(account) && "0".equals(status)) {
					QZMJGameRoom.jinjinAccount.remove(account);
				}
            	if (!QZMJGameRoom.jinjinAccount.contains(account) && "1".equals(status)) {
            		QZMJGameRoom.jinjinAccount.add(account);
				}
            	
            	break;
            case 13:
            default:
                break;
            case 14:
                this.qzmjGameEventDeal.gameTrustee(client, data);
                break;
            case 15:
                this.qzmjGameEventDeal.forcedRoom(data);
                break;
            case 16:
                this.qzmjGameEventDeal.gameReady(client, data);
        }

    }

    private void BDZEvents(Object data, Integer sorts, SocketIOClient client) {
        switch(sorts) {
            case 1:
                this.bdxGameEventDealNew.xiaZhu(client, data);
                break;
            case 2:
                this.bdxGameEventDealNew.gameEvent(client, data);
                break;
            case 3:
                this.bdxGameEventDealNew.exitRoom(client, data);
                break;
            case 4:
                this.bdxGameEventDealNew.reconnectGame(client, data);
        }

    }

    private void ZJHEvents(Object data, Integer sorts, SocketIOClient client) {
        switch(sorts) {
            case 1:
                this.zjhGameEventDealNew.gameReady(client, data);
                break;
            case 2:
                this.zjhGameEventDealNew.gameEvent(client, data);
                break;
            case 3:
                this.zjhGameEventDealNew.exitRoom(client, data);
                break;
            case 4:
                this.zjhGameEventDealNew.reconnectGame(client, data);
                break;
            case 5:
                this.zjhGameEventDealNew.closeRoom(client, data);
                break;
        }

    }

    private void SSSEvents(Object data, Integer sorts, SocketIOClient client) {
        switch(sorts) {
            case 1:
                this.sssGameEventDealNew.gameReady(client, data);
                break;
            case 2:
                this.sssGameEventDealNew.gameEvent(client, data);
                break;
            case 3:
                this.sssGameEventDealNew.exitRoom(client, data);
                break;
            case 4:
                this.sssGameEventDealNew.reconnectGame(client, data);
                break;
            case 5:
                this.sssGameEventDealNew.closeRoom(client, data);
                break;
            case 6:
                this.sssGameEventDealNew.gameBeBanker(client, data);
                break;
            case 7:
                this.sssGameEventDealNew.gameXiaZhu(client, data);
                break;
            case 8:
                this.sssGameEventDealNew.gameStart(client, data);
                break;
            case 9:
                this.sssGameEventDealNew.forcedRoom(data);
                break;
            case 10:
                this.sssGameEventDealNew.checkMyCard(client, data);
                break;
            case 11:
                this.sssGameEventDealNew.handReview(client, data);
                break;
            case 12:
                this.sssGameEventDealNew.relist(client, data);
                break;
            case 13:
                this.sssGameEventDealNew.cutCard(client, data);
                break;
            case 14:
            	this.sssGameEventDealNew.cardEnd(client,data);
            	break;
            case 15:
            	System.out.println("表情事件");
            	this.sssGameEventDealNew.interactive(client, data);
            	break;
        }

    }

    private void NNEvents(Object data, Integer sorts, SocketIOClient client) {
        switch(sorts) {
            case 1:
                this.nnGameEventDealNew.gameReady(client, data);
                break;
            case 2:
                this.nnGameEventDealNew.gameQiangZhuang(client, data);
                break;
            case 3:
                this.nnGameEventDealNew.gameXiaZhu(client, data);
                break;
            case 4:
                this.nnGameEventDealNew.showPai(client, data);
                break;
            case 5:
                this.nnGameEventDealNew.exitRoom(client, data);
                break;
            case 6:
                this.nnGameEventDealNew.reconnectGame(client, data);
                break;
            case 7:
                this.nnGameEventDealNew.closeRoom(client, data);
                break;
            case 8:
                this.nnGameEventDealNew.gameBeBanker(client, data);
                break;
            case 9:
                this.nnGameEventDealNew.gameStart(client, data);
        }

    }

    private void baseEvents(Object data, Integer sorts, SocketIOClient client) {
        switch(sorts) {
            case 1:
                this.baseEventDeal.getUserInfo(client, data);
                break;
            case 2:
                this.baseEventDeal.checkUser(client, data);
                break;
            case 3:
                this.baseEventDeal.getGameSetting(client, data);
                break;
            case 4:
                this.baseEventDeal.getAllRoomList(client, data);
                break;
            case 5:
                this.baseEventDeal.createRoomBase(client, data);
                break;
            case 6:
                this.baseEventDeal.joinRoomBase(client, data);
                break;
            case 7:
                this.baseEventDeal.getShuffleInfo(client, data);
                break;
            case 8:
                this.baseEventDeal.doShuffle(client, data);
                break;
            case 9:
                this.baseEventDeal.sendMessage(client, data);
                break;
            case 10:
                this.baseEventDeal.sendVoice(client, data);
                break;
            case 11:
                this.baseEventDeal.getUserGameLogs(client, data);
                break;
            case 12:
                this.baseEventDeal.dissolveRoom(client, data);
            case 13:
            case 17:
            default:
                break;
            case 14:
                this.baseEventDeal.sendNotice(client, data);
                break;
            case 15:
                this.baseEventDeal.getNotice(client, data);
                break;
            case 16:
                this.baseEventDeal.getRoomAndPlayerCount(client, data);
                break;
            case 18:
                this.baseEventDeal.getRoomGid(client, data);
                break;
            case 19:
                this.baseEventDeal.joinCoinRoom(client, data);
                break;
            case 20:
                this.baseEventDeal.getRoomCardPayInfo(client, data);
                break;
            case 21:
                this.baseEventDeal.getCoinSetting(client, data);
                break;
            case 22:
                this.baseEventDeal.checkSignIn(client, data);
                break;
            case 23:
                this.baseEventDeal.doUserSignIn(client, data);
                break;
            case 24:
                this.baseEventDeal.getCompetitiveInfo(client, data);
                break;
            case 25:
                this.baseEventDeal.joinCompetitiveRoom(client, data);
                break;
            case 26:
                this.baseEventDeal.gameCheckIp(client, data);
                break;
            case 27:
                this.baseEventDeal.getProxyRoomList(client, data);
                break;
            case 28:
                this.baseEventDeal.dissolveProxyRoom(client, data);
                break;
            case 29:
                this.baseEventDeal.getUserAchievementInfo(client, data);
                break;
            case 30:
                this.baseEventDeal.getPropsInfo(client, data);
                break;
            case 31:
                this.baseEventDeal.userPurchase(client, data);
                break;
            case 32:
                this.baseEventDeal.getAchievementRank(client, data);
                break;
            case 33:
                this.baseEventDeal.getDrawInfo(client, data);
                break;
            case 34:
                this.baseEventDeal.gameDraw(client, data);
                break;
            case 35:
                this.baseEventDeal.getAchievementDetail(client, data);
                break;
            case 36:
                this.baseEventDeal.drawAchievementReward(client, data);
                break;
            case 37:
                this.baseEventDeal.changeRoomBase(client, data);
                break;
            case 38:
                this.baseEventDeal.getRoomCardGameLogList(client, data);
                break;
            case 39:
                this.baseEventDeal.getRoomCardGameLogDetail(client, data);
                break;
            case 40:
                this.baseEventDeal.getClubGameLogList(client, data);
                break;
            case 41:
                this.baseEventDeal.getBackpackInfo(client, data);
                break;
            case 42:
                this.baseEventDeal.checkBindStatus(client, data);
                break;
            case 43:
                this.baseEventDeal.userBind(client, data);
                break;
            case 44:
                this.baseEventDeal.refreshLocation(client, data);
                break;
            case 45:
                this.baseEventDeal.getUserLocation(client, data);
                break;
            case 46:
            	this.baseEventDeal.getSignMessageEvent(client,data);
            	break;
            case 47:
            	this.baseEventDeal.UserSignEvent(client,data);
            	break;
            case 48:
            	System.out.println("更换背景--------------------");
            	this.baseEventDeal.updateGameBgSet(client, data);
            	break;
            case 49:
            	this.baseEventDeal.updateUseNameOrHead(client, data);
            	break;
          
        }

    }

    private void gdyEvents(JSONObject object, Integer sorts, SocketIOClient client) {
        switch(sorts) {
            case 0:
                this.gdyGameEventDeal.gdyTest(client, object);
                break;
            case 1:
                this.gdyGameEventDeal.gameReady(client, object);
                break;
            case 2:
                this.gdyGameEventDeal.gameDouble(client, object);
                break;
            case 3:
                this.gdyGameEventDeal.gamePrompt(client, object);
                break;
            case 4:
                this.gdyGameEventDeal.gameEvent(client, object);
                break;
            case 5:
                this.gdyGameEventDeal.gameTrustee(client, object);
                break;
            case 6:
                this.gdyGameEventDeal.reconnectGame(client, object);
                break;
            case 7:
                this.gdyGameEventDeal.gameContinue(client, object);
                break;
            case 8:
                this.gdyGameEventDeal.exitRoom(client, object);
                break;
            case 9:
                this.gdyGameEventDeal.closeRoom(client, object);
                break;
            case 10:
                this.gdyGameEventDeal.earlyStartGame(client, object);
        }

    }

    private void sgEvents(JSONObject object, Integer sorts, SocketIOClient client) {
        switch(sorts) {
            case 0:
                this.sgGameEventDeal.test(client, object);
                break;
            case 1:
                this.sgGameEventDeal.ready(client, object);
                break;
            case 2:
                this.sgGameEventDeal.robZhuang(client, object);
                break;
            case 3:
                this.sgGameEventDeal.bet(client, object);
                break;
            case 4:
                this.sgGameEventDeal.showCard(client, object);
                break;
            case 5:
                this.sgGameEventDeal.exitRoom(client, object);
                break;
            case 6:
                this.sgGameEventDeal.closeRoom(client, object);
                break;
            case 7:
                this.sgGameEventDeal.earlyStart(client, object);
                break;
            case 8:
                this.sgGameEventDeal.reconnect(client, object);
        }

    }

    private void gzmjEvents(Object data, Integer sorts, SocketIOClient client) {
        switch(sorts) {
            case 1:
                this.gzMjGameEventDeal.sitDown(client, data);
                break;
            case 2:
                this.gzMjGameEventDeal.gameReady(client, data);
                break;
            case 3:
                this.gzMjGameEventDeal.gameBet(client, data);
                break;
            case 4:
                this.gzMjGameEventDeal.gameChooseLack(client, data);
                break;
            case 5:
                this.gzMjGameEventDeal.gameReconnect(client, data);
                break;
            case 6:
                this.gzMjGameEventDeal.gameOutCard(client, data);
                break;
            case 7:
                this.gzMjGameEventDeal.gameEvent(client, data);
                break;
            case 8:
                this.gzMjGameEventDeal.closeRoom(client, data);
                break;
            case 9:
                this.gzMjGameEventDeal.exitRoom(client, data);
        }

    }

    private Object obtainMessageStr(Message message) {
        String messageStr = null;
        if (message != null && message instanceof TextMessage) {
            try {
                TextMessage tm = (TextMessage)message;
                messageStr = tm.getText();
            } catch (JMSException var4) {
                throw new EventException("[" + this.getClass().getName() + "] 信息接收出现异常");
            }
        }

        return messageStr;
    }
}
