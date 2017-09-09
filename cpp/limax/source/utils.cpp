#include "common.h"

namespace limax {
#pragma region uithread tools
	namespace uithread
	{
		static std::vector<Runnable> tasks;
		static std::mutex mutex;
	}

	void runOnUiThread(Runnable task)
	{
		std::lock_guard<std::mutex> l(uithread::mutex);
		uithread::tasks.push_back(task);
	}

	void uiThreadSchedule()
	{
		std::vector<Runnable> tmp;
		{
			std::lock_guard<std::mutex> l(uithread::mutex);
			tmp.swap(uithread::tasks);
		}
		for (auto& task : tmp)
			task();
	}
    
    void uiThreadScheduleTime(int ms)
    {
        std::vector<Runnable> tmp;
        {
            std::lock_guard<std::mutex> l(uithread::mutex);
            tmp.swap(uithread::tasks);
        }
        auto rt = std::chrono::high_resolution_clock::now();
        auto timeouted = false;
        std::vector<Runnable> unrun;
        unrun.reserve(tmp.size());
        for (auto& task : tmp) {
            if( timeouted)
            {
                unrun.push_back(task);
            }
            else
            {
                task();
                auto dt = std::chrono::high_resolution_clock::now() - rt;
                auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(dt).count();
                timeouted = elapsed >= ms;
            }
        }
        if(!unrun.empty()) {
            std::lock_guard<std::mutex> l(uithread::mutex);
            uithread::tasks.insert(uithread::tasks.begin(), unrun.begin(), unrun.end());
        }
    }
    
    void uiThreadScheduleCount(size_t count)
    {
        std::vector<Runnable> tmp;
        {
            std::lock_guard<std::mutex> l(uithread::mutex);
            if( uithread::tasks.size() <= count)
            {
                tmp.swap(uithread::tasks);
            }
            else
            {
                tmp.reserve(count);
                auto b = uithread::tasks.begin();
                auto e = b + count;
                tmp.insert(tmp.begin(), b, e);
                uithread::tasks.erase(b, e);
            }
        }
        for (auto& task : tmp)
            task();
    }
    
#pragma endregion
#pragma region class Resource

	struct Resource::Impl
	{
		std::shared_ptr<Impl> parent;
		std::unordered_set<std::shared_ptr<Impl>> children;
		Runnable cleanup;
		std::mutex mutex;
		Impl();
		Impl(std::shared_ptr<Impl> parent, Runnable _cleanup);
		void _close();
		void close(std::shared_ptr<Impl> self);
	};

	Resource::Impl::Impl() : parent(nullptr), cleanup([](){}){}
	Resource::Impl::Impl(std::shared_ptr<Impl> _parent, Runnable _cleanup) : parent(_parent), cleanup(_cleanup)	{}
	void Resource::Impl::_close()
	{
		std::lock_guard<std::mutex> l(mutex);
		if (!cleanup)
			return;
		for (auto c : children)
			c->_close();
		children.clear();
		cleanup();
		cleanup = nullptr;
	}
	void Resource::Impl::close(std::shared_ptr<Impl> self)
	{
		if (parent)
		{
			std::lock_guard<std::mutex> l(parent->mutex);
			if (parent->children.erase(self))
				_close();
		}
		else
			_close();
	}
	void Resource::close(){ impl->close(impl); }
	Resource::Resource(Resource& parent, Runnable cleanup) {
		std::lock_guard<std::mutex> l(parent.impl->mutex);
		if (!parent.impl->cleanup)
			throw IllegalStateException();
		impl.reset(new Impl(parent.impl, cleanup));
		parent.impl->children.insert(impl);
	}
	void Resource::swap(Resource& rhs) { impl.swap(rhs.impl); }
	Resource::Resource() {}
	Resource::Resource(const Resource& rhs) : impl(rhs.impl) { }
	Resource::Resource(const Resource&& rhs) : impl(std::move(rhs.impl)) { }
	Resource& Resource::operator=(const Resource&rhs) { Resource(rhs).swap(*this);  return *this; }
	Resource& Resource::operator=(const Resource&&rhs){ Resource(std::move(rhs)).swap(*this); return *this; }
	Resource Resource::createRoot(){ Resource r; r.impl.reset(new Impl()); return r; }
	Resource Resource::create(Resource& parent, Runnable cleanup){ return Resource(parent, cleanup); }
#pragma endregion
#pragma region class trace
	Trace::Level Trace::level = Trace::Level::Warn;
	Trace::Destination Trace::printTo = [](const std::string& msg){ printf("%s\n", msg.c_str()); };
#pragma endregion
} // namespace limax

namespace limax {
#pragma region file marshal

#pragma endregion
#pragma region http

#pragma endregion
} // namespace limax
