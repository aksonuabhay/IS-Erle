import json
import requests
import time


session=requests.Session()
url_prefix = "http://127.0.0.1:8080/interactivespaces/"
activity_id = {}#{'captain':'0','serial':'0','udp':'0','mavlink':'0','generator':'0','processor':'0'}

def make_request(url):
    live_activity=requests.get(url).content
    return json.loads(live_activity)

def url_startup(uid):
    return "liveactivity/"+uid+"/startup.json"

def url_activate(uid):
    return "liveactivity/"+uid+"/activate.json"

def url_deactivate(uid):
    return "liveactivity/"+uid+"/deactivate.json"

def url_shutdown(uid):
    return "liveactivity/"+uid+"/shutdown.json"

def url_deploy(uid):
    return "liveactivity/"+uid+"/deploy.json"

def url_status(uid):
    return "liveactivity/"+uid+"/status.json"

def process_result(result_data):
    if result_data['result']=='success':
        print "Success"
        return 'success'
    else:
        print "Could not contact master properly"
        return 'fail'

def launch_sequence(comms_port):
    if process_result(make_request(url_prefix+url_startup(activity_id['mavlink']))) == 'success':
        time.sleep(3)
        if process_result(make_request(url_prefix+url_startup(activity_id[comms_port]))) == 'success':
            time.sleep(1)
            if process_result(make_request(url_prefix+url_startup(activity_id['generator']))) == 'success':
                time.sleep(1)
                if process_result(make_request(url_prefix+url_activate(activity_id['mavlink']))) == 'success':
                    time.sleep(0.5)
                    if process_result(make_request(url_prefix+url_activate(activity_id[comms_port]))) == 'success':
                        time.sleep(0.5)
                        if process_result(make_request(url_prefix+url_activate(activity_id['generator']))) == 'success':
                            time.sleep(2)
                            if process_result(make_request(url_prefix+url_startup(activity_id['captain']))) == 'success':
                                time.sleep(2)
                                if process_result(make_request(url_prefix+url_activate(activity_id['captain']))) == 'success':
                                    time.sleep(0.5)
                                    if process_result(make_request(url_prefix+url_startup(activity_id['processor']))) == 'success':
                                        time.sleep(1)
                                        if process_result(make_request(url_prefix+url_activate(activity_id['processor']))) == 'success':
                                            time.sleep(0.5)
                                            print 'Launched all the activities successfully'
#url_live_activity = "http://127.0.0.1:8080/interactivespaces/liveactivity/all.json";
#live_activity=requests.get(url_prefix+"liveactivity/all.json").content
#print live_activity
data= make_request(url_prefix+"liveactivity/all.json")


if data['result']=='success':
    for i in range(0,len(data['data']),1):
        if data['data'][i]['activity']['identifyingName']== 'is.erle.captain':
            activity_id['captain'] = data['data'][i]['id']
        elif data['data'][i]['activity']['identifyingName']== 'is.erle.comm.serial':
            activity_id['serial'] = data['data'][i]['id']
        elif data['data'][i]['activity']['identifyingName']== 'is.erle.comms':
            activity_id['udp'] = data['data'][i]['id']
        elif data['data'][i]['activity']['identifyingName']== 'is.erle.mavlink':
            activity_id['mavlink'] = data['data'][i]['id']
        elif data['data'][i]['activity']['identifyingName']== 'is.erle.waypoint.generator':
                activity_id['generator'] = data['data'][i]['id']
        elif data['data'][i]['activity']['identifyingName']== 'is.erle.waypoint.processor':
            activity_id['processor'] = data['data'][i]['id']
    # print data['data'][9]
    #print activity_id['captain']
    if len(activity_id)>=5:
        print 'Starting all the activities'
        launch_sequence('serial')
    else:
        print 'Not all the activities are installed'
        print 'Only the following activities are installed, Install rest of them'
        print activity_id
else:
    print "Could not contact master properly"

