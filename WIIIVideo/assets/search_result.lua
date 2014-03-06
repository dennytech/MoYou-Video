local Version = "1.0.20140306.1139"

local Log = luajava.bindClass("com.dennytech.common.util.Log")
local Toast = luajava.bindClass("android.widget.Toast")
local Video = luajava.bindClass("com.dennytech.wiiivideo.data.Video")
local Gson = luajava.bindClass("com.google.gson.Gson")
local context

Log:i("lua", "Version:" .. Version)

function init(ctx)
  Log:i("lua", "init")
  context = ctx
end

function parse(source)
	Log:i("lua", "parse start")

    local recommend = luajava.newInstance("java.util.ArrayList")
	local list = luajava.newInstance("java.util.ArrayList")

	local Jsoup = luajava.bindClass("org.jsoup.Jsoup")
	local doc = Jsoup:parse(source)

	-- recommend
	express = doc:getElementsByClass("sk-express")
	if express and express:size() > 0 then
		local recombox = express:get(0):getElementsByClass("recom_box")
		local li = recombox:get(0):getElementsByTag("li")
		local size = li:size()
		for i=0,size do
			local element = li:get(i)
		    local video = luajava.newInstance("com.dennytech.wiiivideo.data.Video")
			local pic = element:getElementsByClass("pic"):get(0)
			video:setTitle(pic:attr("title"))
			video:setId(pic:attr("_log_vid"))
			video:setThumb(pic:getElementsByTag("img"):get(0):attr("src"))
				
			local span = element:getElementsByTag("span")
			video:setLength(span:get(0):text())
			video:setPlayTimes(span:get(1):text())
			video:setPublishTime(span:get(2):text())
			recommend:add(video)
			if i == (size - 2) then
        	    break
            end
		end
	end

	-- list
	local vlist = doc:getElementsByClass("sk-vlist"):get(0)
	local v = vlist:getElementsByClass("v")
    local size = v:size()
	for i=0,size do
		local element = v:get(i)
		local video = luajava.newInstance("com.dennytech.wiiivideo.data.Video")
		local vthumb = element:getElementsByClass("v-thumb"):get(0)
		video:setThumb(vthumb:getElementsByTag("img"):attr("src"))
		video:setLength(vthumb:getElementsByClass("v-time"):get(0):childNode(0):toString())
		local vmeta = element:getElementsByClass("v-meta"):get(0)
		video:setTitle(vmeta:getElementsByTag("a"):get(0):attr("title"))
		local id = vmeta:getElementsByTag("a"):get(0):attr("href");
		id = id:gsub("http://v.youku.com/v_show/id_", "")
		id = id:gsub(".html", "")
		video:setId(id)
		local vmetadata = vmeta:getElementsByClass("v-meta-data")
		video:setPlayTimes(vmetadata:get(1):getElementsByTag("span"):text())
		video:setPublishTime(vmetadata:get(2):getElementsByTag("span"):text())
        list:add(video)
        if i == (size - 1) then
        	break
        end
    end

    local vl = luajava.newInstance("com.dennytech.wiiivideo.data.VideoList")
    vl:setList(list)
    vl:setRecommend(recommend)

    local gson = luajava.newInstance("com.google.gson.Gson")
    local jsonStr = gson:toJson(vl)

    Log:i("lua", "parse end")

    return jsonStr
end