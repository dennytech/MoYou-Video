local Log = luajava.bindClass("com.dennytech.common.util.Log")
local Toast = luajava.bindClass("android.widget.Toast")
local Video = luajava.bindClass("com.dennytech.wiiivideo.data.Video")
local Gson = luajava.bindClass("com.google.gson.Gson")
local context

function init(ctx)
  Log:i("lua", "init start")
  context = ctx
end

function parse(source)
	Log:i("lua", "parse start")

	local list = luajava.newInstance("java.util.ArrayList")

	local Jsoup = luajava.bindClass("org.jsoup.Jsoup")
	local doc = Jsoup:parse(source)
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

    local gson = luajava.newInstance("com.google.gson.Gson")
    local jsonStr = gson:toJson(list)

    Log:i("lua", "parse end")

    return jsonStr
end